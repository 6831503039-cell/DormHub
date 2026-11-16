# DormHub: Where Each Grading Item Is in the Code

Based on the three guideline slides: tools, database and UI ("What do you need"), the evaluation table (30 points), and the submission slide. All paths and line numbers are from the latest `main` (commit `6f39033`, 14 Nov). If a teammate edits a file, the line numbers can shift by a few lines, so open the file once before you present.

| Item | Points | Status | Main evidence |
|---|---|---|---|
| Spring Boot + Spring Data JPA | condition | Done | `pom.xml:9`, `App.java:6` |
| Database (H2 and Cloud SQL MySQL) | condition | Done | `application.properties:17-23` |
| UI with CSS or Bootstrap | condition | Done | `fragments/admin.html:13-14`, `static/css/app.css` |
| Listing / Searching | 2 | Done | `RoomController.java:49-54`, `TenantController.java:52-56` |
| Creating | 2 | Done | `RoomController.java:121-131` |
| Updating | 2 | Done | `RepairController.java:50-65`, `PaymentController.java:53-65` |
| Deleting | 2 | Done | `RoomController.java:133-142` |
| Managing related entities | 2 | Done | `TenantController.java:91-111` |
| Entity classes (5 related) | 10 | Done, 11 entities | `domain/Room.java:39-52` and others |
| Front-end | 5 | Done | `templates/` (35 template files), `app.css` (893 lines) |
| Deploy on GCP | 5 | Done | `Dockerfile`, `cloudbuild.yaml`, `application.properties:10-15` |

---

## 1. Tools and framework

**Spring Boot.** `pom.xml:8-9` uses `spring-boot-starter-parent` version `2.7.18`. The main class is `src/main/java/th/mfu/App.java:6` with `@SpringBootApplication`.

**Spring Data JPA.** `pom.xml:32` has `spring-boot-starter-data-jpa`. There are 11 repository interfaces in `src/main/java/th/mfu/` (for example `RoomRepository.java`, `TenantRepository.java`, `BillRepository.java`).

**What to say:** "We used Spring Boot 2.7 for the web server and Spring Data JPA for the database. A repository interface is enough, Spring writes the SQL."

## 2. Database: H2 and Cloud SQL (MySQL)

File `src/main/resources/application.properties`:

```properties
17: spring.datasource.url=${DB_URL:jdbc:h2:mem:dormhub}
18: spring.datasource.username=${DB_USER:sa}
19: spring.datasource.password=${DB_PASSWORD:password}
23: spring.jpa.hibernate.ddl-auto=update
```

Drivers in `pom.xml`: H2 at lines 43-44, MySQL at 49-50, and the Cloud SQL socket factory at line 56.

**What to say:** "The same code supports both databases from the slide. With no settings, it runs on an embedded H2 database, which is how you run it on your laptop. In production Cloud Run sets `DB_URL`, `DB_USER` and `DB_PASSWORD`, and the app uses MySQL on Cloud SQL. The password is never in the code."

Honest note: the Cloud SQL connection string is in a comment at `application.properties:15`. It contains the instance name but no password.

## 3. UI with CSS and Bootstrap

- Bootstrap 5.3.3 is loaded in `templates/fragments/admin.html:13` and in `auth/login.html:9`, `auth/register.html:9`.
- Our own stylesheet is `static/css/app.css` (893 lines), linked at `fragments/admin.html:14`. It has phone rules such as `@media (max-width: 575.98px)` at line 301 and touch-size rules at line 235.
- Lucide icons: `data-lucide` icons in the navigation (`fragments/admin.html:25-30`).
- Thai and English text come from `messages.properties` and `messages_en.properties`.

**What to say:** "The pages use Bootstrap for the grid and forms, and about 900 lines of our own CSS for the dorm look, the light and dark theme, and the phone layout."

---

## 4. Controls (10 points, 2 each)

Our controllers follow the same pattern for each entity. Use the Room and Tenant ones for the demo.

### 4.1 Listing / Searching (2)

`admin/RoomController.java`
```java
49: @GetMapping("/rooms")
50: public String listRooms(@RequestParam(required = false) String q, Model model) {
53:     List<Room> rooms = keyword.isEmpty() ? repository.findAllByOrderByRoomNumberAsc()
54:             : repository.findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCaseOrderByRoomNumberAsc(keyword, keyword);
```

`RoomRepository.java`: the search is a Spring Data **derived query**. Spring builds `WHERE LOWER(roomNumber) LIKE %q% OR LOWER(roomType) LIKE %q% ORDER BY roomNumber` from the method name.

`admin/TenantController.java:52-56` calls `repository.search(keyword)`, and `TenantRepository.java` defines it as a **JPQL query** (`@Query("SELECT t FROM Tenant t LEFT JOIN t.room r WHERE ...")`). It matches first name, last name, full name, phone, email or room number, and the `LEFT JOIN` keeps tenants who have no room.

The search box is in `templates/admin/rooms/list.html:26-30` (a GET form with `name="q"`).

**What to say:** "Searching runs in the database, not in Java. Rooms use a derived query, where the method name is the query. Tenants use a JPQL query that also joins the room, so you can find a tenant by room number."

**Demo:** open `/admin/rooms?q=A1` and `/admin/tenants?q=somchai`.

### 4.2 Creating (2)

`RoomController.java`
```java
121: @PostMapping("/rooms")
122: public String saveRoom(@ModelAttribute Room room, RedirectAttributes redirect) {
124:     repository.save(room);
125: } catch (DataIntegrityViolationException e) {   // duplicate room number
```

The form page is `GET /rooms/new` at `RoomController.java:106-107`. The same pattern exists for tenants (`TenantController.java:81`), bills (`BillController.java:51-75`), repairs (`RepairController.java:39`), parcels (`ParcelController.java:54`) and announcements.

**What to say:** "Create is a form, then a POST. `save()` inserts the row. If the room number already exists, the database rejects it and we show a friendly error instead of a crash."

### 4.3 Updating (2)

Updating uses the same endpoints: when the form carries an `id`, `save()` updates instead of inserts (`RoomController.java:112-114` loads the room for editing, `121-124` saves it).

Two clearer examples:
- `RepairController.java:50-65`: for an existing repair, only the status and the admin note are changed (lines 57-61); what the tenant reported stays as sent.
- `PaymentController.java:53-65`: `approve` and `reject` change a payment's status. `BillController.java:77-83` toggles a bill between paid and unpaid.

**What to say:** "Update loads the existing record, changes only the allowed fields, and saves it. For repairs the owner can change the status and add a note, but not rewrite what the tenant reported."

### 4.4 Deleting (2)

`RoomController.java`
```java
133: @GetMapping("/rooms/{id}/delete")
136:     repository.deleteById(id);
138: } catch (DataIntegrityViolationException e) {
139:     redirect.addFlashAttribute("error", messages.get("flash.room.inUse"));
```

Careful deletes: `TenantController.java:113-124` keeps the payment history by detaching it from the tenant (lines 117-120) before deleting; `BillController.java:85-92` deletes the bill's payments first (line 88). The delete buttons ask for confirmation first (non-functional requirement 18).

**What to say:** "Deleting respects the relationships. A room that still has data cannot be deleted. When we delete a tenant we keep the payment records. When we delete a bill, its slips are deleted with it."

### 4.5 Managing related entities (2)

`TenantController.java:91-111` is the best example.

```java
94: Room oldRoom = tenant.getId() == null ? null : findTenant(tenant.getId()).getRoom();
95: Room newRoom = tenant.getRoom();
97: if (newRoom != null && isFull(newRoom, oldRoom)) { ... error: room full ... }
103: repository.save(tenant);
104: updateRoomStatus(newRoom);
105: if (oldRoom != null && (newRoom == null || !oldRoom.getId().equals(newRoom.getId()))) {
106:     updateRoomStatus(oldRoom);
```

`TenantController.java:148-154`: a room becomes OCCUPIED while it has at least one tenant and AVAILABLE when it has none (rooms under maintenance are left alone).

Other places: the tenant form picks a room (`TenantController.java:138`), a bill or parcel is attached to a room, `BillController.java:63-75` copies the room's monthly rent into the bill, and `PaymentController.java:53-58` links a slip to its bill and updates the bill's paid status.

**What to say:** "When you assign a tenant to a room, the system checks the room capacity, saves the tenant, and updates the status of the new room and of the old room. So the entities stay consistent without the owner doing it by hand."

---

## 5. Entity classes: 5 related entities (10 points)

We have **11 `@Entity` classes** in `src/main/java/th/mfu/domain/`: Room, Tenant, Bill, Payment, BookingRequest, RepairRequest, Parcel, Feedback, AppUser, Announcement, LaundryMachine.

Correction for you: an earlier message in your chat said 12 entities. The code on `main` has **11**. Say eleven.

Relationships (all verified):

| Relationship | Code |
|---|---|
| Room → many Tenant | `Room.java:39` `@OneToMany(mappedBy = "room")`, `Tenant.java:34` `@ManyToOne` |
| Room → many Bill | `Room.java:45`, `Bill.java:28` `@ManyToOne(optional = false)` |
| Room → many BookingRequest | `Room.java:51`, `BookingRequest.java:29` |
| Bill → many Payment | `Bill.java:33` `@OneToMany(mappedBy = "bill")`, `Payment.java:27` `@ManyToOne(optional = false)` |
| Payment → Tenant | `Payment.java:32` `@ManyToOne` (null if the tenant was deleted) |
| Parcel → Room | `Parcel.java:26` |
| RepairRequest → Room | `RepairRequest.java:23` |
| Feedback → AppUser and Room | `Feedback.java:28` and `32` |
| BookingRequest → AppUser | `BookingRequest.java:34` |

Snippet to show (`Room.java:38-42`):
```java
// one room -> many tenants; "mappedBy" = Tenant.room owns the room_id column
@OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
@OrderBy("firstName ASC")
private List<Tenant> tenants;
```

**What to say:** "We have eleven entities. The five main related ones are Room, Tenant, Bill, Payment and BookingRequest. A room has many tenants, bills and booking requests, and a bill has many payments. Both sides use `@OneToMany` and `@ManyToOne`, and the many side owns the foreign key."

Draw this chain on a slide: **Room → Tenant**, **Room → Bill → Payment**, **Room → BookingRequest**.

## 6. Front-end (5 points)

- 35 Thymeleaf template files in `src/main/resources/templates/` (admin, app, auth, explore, fragments).
- Shared layout pieces in `templates/fragments/` (`admin.html` is the owner layout, `fonts.html` the fonts).
- JavaScript in `static/js/`: `ui.js`, `prefs.js` (language and theme), `password-toggle.js`, and `dorm3d.js` (the owner's 3D room map).
- Thai and English with `messages*.properties`, light and dark theme, and a phone layout with a bottom menu for tenants.

**What to say:** "The front end is Thymeleaf pages with Bootstrap and our own CSS. It works on phones, has a Thai and English switch, a dark theme, and a 3D room map for the owner."

## 7. Deployment on GCP (5 points)

- `Dockerfile` (two stages): lines 2-5 build the jar with Maven and Java 17, lines 8-10 run it on a small Java runtime.
- `cloudbuild.yaml`: builds the image, pushes it to Artifact Registry, and runs `gcloud run deploy dormhub` on Cloud Run (region `us-central1`, port `8100`, `--allow-unauthenticated`, `--max-instances 1`, `--session-affinity`).
- Cloud SQL: `application.properties:10-15` shows the `jdbc:mysql:///dormhub?cloudSqlInstance=...&socketFactory=com.google.cloud.sql.mysql.SocketFactory` URL, and `pom.xml:56` adds the connector. The database password is set in Cloud Run, not in the code.
- Live site: https://dormhub-621353901508.us-central1.run.app

**What to say:** "Every push to GitHub triggers Cloud Build, which builds a Docker image and deploys it to Cloud Run. The app talks to a MySQL database on Cloud SQL. We run one instance with session affinity because login sessions are kept in memory."

## 8. Submission slide checks

- **README with the project description, Cloud Run link, member names and IDs:** done, all five members are listed and the link is at the top.
- **No password or API key in the repo:** mostly true. The database password and the Gemini key are environment variables. The demo admin and tenant passwords are visible on purpose (README and `application.properties:dormhub.admin.password`), and the PromptPay ID is in `application.properties`. Say that they are demo values.
- **Every member must commit:** check the contribution count before presenting.

## 9. Suggested order for this part of the presentation (about 4 minutes)

1. Show the rubric table above (10 seconds): "Everything on the slide is covered."
2. Show the `Room` and `Tenant` entities and the relationship diagram (40 seconds).
3. Live demo on the site: search a room, add a room, edit it, delete it, then assign a tenant and watch the room become Occupied (90 seconds).
4. Show `TenantController.java:91-111` for managing related entities (40 seconds).
5. Show `application.properties` and `cloudbuild.yaml` for the database and deployment (40 seconds).
