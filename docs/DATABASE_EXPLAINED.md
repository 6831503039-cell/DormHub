# DormHub Database: Explanation, Evaluation and Hard Questions

Everything here is read from the code on `main` (entities in `src/main/java/th/mfu/domain/`, repositories in `src/main/java/th/mfu/`, `application.properties`, `DataSeeder.java`). I did not connect to the live Cloud SQL database, so I did not check the real tables, indexes or row counts. Check those before you claim numbers.

## 1. The big picture

- **Engine:** MySQL on Google Cloud SQL in production, an embedded in-memory H2 database when `DB_URL` is not set (`application.properties:17-19`).
- **Access:** Spring Data JPA with Hibernate. No SQL files. The tables are created from the `@Entity` classes.
- **Schema management:** `spring.jpa.hibernate.ddl-auto=update` (line 23): Hibernate adds missing tables and columns and keeps data.
- **Sample data:** `DataSeeder` creates demo rooms, tenants and 12 months of bills, but only when the `room` table is empty (`DataSeeder.java:90`), so MySQL data survives restarts. It runs inside one transaction (`:87`), so an interrupted start leaves nothing half-seeded. The admin account is checked on every start (`:89`).
- **Naming:** `PhysicalNamingStrategyStandardImpl` is set, so column names equal the Java field names (`roomNumber`, `billMonth`), not snake_case.
- **IDs:** every table has `Long id` with `@GeneratedValue(strategy = AUTO)`. With `use-new-id-generator-mappings=false`, each table has its own counter (rooms 1,2,3, bills 1,2,3) instead of one shared sequence.

## 2. The 11 tables

| Entity | Key columns | Relationships (foreign keys) |
|---|---|---|
| **Room** | `roomNumber` (unique, not null), building, floor, roomType, monthlyRent, capacity, status (enum) | One room has many Tenant, Bill and BookingRequest |
| **Tenant** | firstName, lastName, phone, email, checkInDate, contractEndDate | `room_id`, optional: a tenant may have no room yet |
| **Bill** | billMonth ("2026-11"), rent, water and electric units and rates, `paid`, dueDate | `room_id`, required |
| **Payment** | amount, submittedAt, status (enum), aiNote (1000), slip image (BLOB) | `bill_id` required, `tenant_id` optional (kept if the tenant is deleted) |
| **BookingRequest** | fullName, phone, email, moveInDate, note (500), createdAt, status | `room_id`, `applicant_id` (to AppUser), both optional |
| **RepairRequest** | title, description (1000), reportedDate, status, reportedBy, adminNote | `room_id`, required |
| **Parcel** | recipientName, carrier, note, arrivedAt, pickedUpAt, pickedUpBy, photo (BLOB) | `room_id`, required |
| **Feedback** | category, rating, message (1000), createdAt, readByOwner | `author_id` (AppUser) required, `room_id` optional |
| **AppUser** | `email` (unique, not null), password (BCrypt hash), fullName, role (ADMIN or TENANT), avatar (BLOB) | none |
| **Announcement** | title (200, not null), body (4000), pinned, createdAt, image (BLOB) | none |
| **LaundryMachine** | name, type (enum), busyUntil, usedBy | none |

Relationship chain to remember: **Room → Tenant**, **Room → Bill → Payment**, **Room → BookingRequest**, plus Parcel, RepairRequest and Feedback hanging off Room.

*(Note: the foreign key column names are `room_id`, `bill_id` and so on, because of how Hibernate names join columns. Confirm in the live database before you quote them.)*

## 3. Design decisions you can defend

1. **One side owns each relationship.** `@OneToMany(mappedBy = "room")` on Room and `@ManyToOne` on Tenant: the many side holds the foreign key (`Room.java:39`, `Tenant.java:34`). This is the standard JPA pattern and avoids join tables.
2. **Lazy loading.** All `@OneToMany` use `FetchType.LAZY` and the BLOB columns use `@Basic(fetch = LAZY)`, so listing rooms does not load every slip image.
3. **Enums stored as text** (`@Enumerated(EnumType.STRING)`): readable in the database and safe if the enum order changes.
4. **Snapshots on purpose.** A bill stores its own water and electric *rates* (`waterRate = 20.0`, `electricRate = 7.0`). If the price changes later, old bills do not change. A booking request copies the applicant's name, phone and email for the same reason.
5. **History is kept.** `Payment.tenant` is optional: deleting a tenant detaches the payments instead of deleting them (`TenantController.java:117-121`). A bill's payments are deleted together with the bill (`BillController.java:88`).
6. **Unique constraints where it matters.** `Room.roomNumber` and `AppUser.email` are unique, so duplicates are rejected by the database itself, and the controller turns the error into a friendly message (`RoomController.java:125-127`).
7. **Search in the database.** Derived queries and one JPQL query (see section 5), not filtering in Java.
8. **Environment-based connection.** The same code runs on H2 or Cloud SQL, with the password never in the repository.

## 4. Honest weaknesses (say them before they ask)

| # | Weakness | Evidence | Why it matters | Fix |
|---|---|---|---|---|
| 1 | **Money is `Double`** | `Bill.rent`, `waterRate`, `Payment.amount`, `Room.monthlyRent` | Floating point cannot store 0.10 exactly; the code rounds shares to 2 decimals and allows 0.50 baht tolerance to hide this | `BigDecimal` and a `DECIMAL(10,2)` column |
| 2 | **No unique rule for one bill per room per month** | `billMonth` is a plain `String`; no `@UniqueConstraint` found | The owner can create two bills for the same room and month | `@Table(uniqueConstraints = ...)` on `(room_id, billMonth)` |
| 3 | **Month is a String** | `Bill.billMonth = "2026-11"` | Works because the format sorts correctly, but the database cannot validate it | `YearMonth` converter or a date column |
| 4 | **No explicit indexes** | no `@Index` found | Foreign key columns get an index automatically in MySQL InnoDB, but searches such as `LIKE '%q%'` cannot use an index at all | Fine for 220 rooms; at scale use full-text search |
| 5 | **Tenants and accounts are linked by email, not by a foreign key** | `AppUser` has no relation to `Tenant` | If the owner mistypes the email, the tenant sees nothing; changing an email breaks the link | Add `Tenant.user` (`@OneToOne`) |
| 6 | **No `@Version`, no explicit transactions in the controllers** | `@Transactional` appears only in `DataSeeder` | Saving a tenant and then updating two room statuses are separate commits; a crash in between leaves a wrong status. Two owners editing at the same time overwrite each other | `@Transactional` on service methods, `@Version` for optimistic locking |
| 7 | **Derived data is stored** | `Room.status` is set by code (`TenantController.java:148-154`); `Bill.paid` is set from payments (and can be toggled by hand, `BillController.java:77-83`) | Redundant data can disagree with the real data | Compute it, or keep it but update it in one transaction |
| 8 | **Images live in the database** | `@Lob byte[]` for slips, parcel photos, avatars, announcement images | Backups and queries get heavy as images grow; the limit is 5 MB per upload | Cloud Storage with only the URL in the table |
| 9 | **No cascade rules and no orphan removal** | no `CascadeType` found | Every delete order is written by hand in controllers; a missed case gives a foreign key error | Cascade where the child cannot exist alone |
| 10 | **`ddl-auto=update`** | `application.properties:23` | It never drops or renames columns and gives no migration history | Flyway or Liquibase |
| 11 | **Free-text columns instead of references** | `RepairRequest.reportedBy`, `Parcel.recipientName`, `Parcel.pickedUpBy`, `LaundryMachine.usedBy`, `Feedback.category` | Names can differ in spelling and are not linked to the Tenant table | Reference the tenant, or a category enum |
| 12 | **Demo data in production** | `DataSeeder` | The live database holds demo rooms and bills | A profile flag so the seeder runs only for demos |

## 5. Queries and how they run

| Repository method | What it does |
|---|---|
| `RoomRepository.findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCaseOrderByRoomNumberAsc` | Search: `WHERE LOWER(roomNumber) LIKE %q% OR LOWER(roomType) LIKE %q% ORDER BY roomNumber`, built from the method name |
| `TenantRepository.search` (`@Query`, JPQL) | `LEFT JOIN t.room`, matches name, full name, phone, email or room number; the join keeps tenants without a room |
| `TenantRepository.countByRoom` | Used for capacity and room status |
| `BillRepository.findByPaidFalseOrderByBillMonthAsc` | Unpaid bills for reminders |
| `PaymentRepository.countByStatusIn(...)` | Dashboard count of slips to review |
| `ParcelRepository.countByPickedUpAtIsNull` | Parcels not yet picked up |
| `AppUserRepository.findByEmailIgnoreCase` / `existsByEmailIgnoreCase` | Login and registration |

## 6. Walk-through you can narrate: "one payment from start to finish"

1. A **Bill** exists for a **Room** and month (`Bill.room`, required).
2. The tenant uploads a slip: a new **Payment** row with `bill_id`, `tenant_id`, `amount`, the image as a BLOB, status `PENDING`.
3. The AI check fills `aiNote` and may set the status to approved, pending or suspicious.
4. The owner approves or rejects (`PaymentController.java:53-65`).
5. `BillingService` marks the **Bill** paid when approved payments reach the total minus 0.50 baht.
6. If the tenant is deleted later, `Payment.tenant` becomes `NULL` and the history stays.

## 7. Hard questions about the database

**Why JPA and not plain SQL?** Less code, the same code works on H2 and MySQL, and the relationships are declared once on the entities. Trade-off: we depend on Hibernate's naming and fetching rules.

**Why two databases?** H2 lets anyone run the project with one command and no installation. MySQL on Cloud SQL gives real persistence in production. The switch is only the `DB_URL` variable.

**Is it normalised?** Mostly third normal form: each fact is stored once and linked by foreign keys. The exceptions are on purpose (rates and applicant details copied as snapshots) or are weaknesses we listed (derived status fields, free-text names).

**What stops two rooms with the same number or two accounts with the same email?** `unique = true` on `Room.roomNumber` and `AppUser.email`. The database rejects it, and the controller shows a message.

**What happens if you delete a room that has tenants?** `deleteById` fails with a `DataIntegrityViolationException` because of the foreign key, and the owner sees "room in use" (`RoomController.java:138-139`).

**What happens to payments when a tenant is deleted?** They stay. The `tenant_id` is set to null first (`TenantController.java:117-120`).

**How do you avoid loading everything?** Lazy relationships and lazy BLOBs, plus search queries that filter in the database.

**Is it safe from SQL injection?** Yes for our queries: they are derived queries or JPQL with a named parameter (`:q`), so Hibernate uses prepared statements. We never build SQL by joining strings.

**What if the database is down?** The app cannot work without it; the pages show an error. There is no replica or cache. Cloud SQL provides backups if enabled, which we did not verify.

**Can it handle more tenants or rooms?** The demo has about 220 rooms and a year of bills, which is small for MySQL. The first limits would be the single Cloud Run instance, the in-memory sessions and image BLOBs, not the row counts.

**Where are the passwords?** In `AppUser.password` as BCrypt hashes. The database password is an environment variable.

**Why `Double` for money?** Say it plainly: it was simple and we round to 2 decimals and use a 0.50 baht tolerance, but `BigDecimal` with `DECIMAL(10,2)` is the correct choice for a real product.

**Why store images in the database?** One system to deploy and back up, and no extra service for a course project. It does not scale well; Cloud Storage would be the next step.

**How would you change the schema safely?** Today Hibernate `update` adds columns automatically. For production, use Flyway migrations and test them on a copy of the data.

## 8. One-minute summary to say aloud

"DormHub has eleven tables in MySQL on Cloud SQL, and H2 when you run it locally. The core is Room: a room has tenants, bills and booking requests, and each bill has payments. We use Spring Data JPA, so the relationships are declared on the entities and the search runs in the database. We made some careful choices, like keeping payment history when a tenant is deleted and storing the rates inside each bill. We also know what we would improve: BigDecimal for money, a unique bill per room and month, transactions around multi-step changes, and migrations instead of auto-update."
