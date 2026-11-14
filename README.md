# DormHub(Dormitory Management System)

DormHub is a web application for managing a dormitory, used by both the dorm owner and the tenants. The owner manages rooms, tenants, monthly bills (rent, water and electricity), payments, repair requests, parcels and announcements. Tenants view and pay their bills with a QR code and upload the payment slip, report repairs, pick up parcels, use the laundry machines, book a vacant room and ask questions to a chatbot.

- **Google Cloud Run:** https://dormhub-621353901508.us-central1.run.app
- **Repository:** https://github.com/6831503039-cell/DormHub

## Group Members

| # | Student ID | Name |
|---|---|---|
| 1 | 6831503034 |Kanticha Sriruang |
| 2 | 6831503037 |Chawintorn Thanapreechanan |
| 3 | 6831503039 |Yanavut Thidnongwaeng |
| 4 | 6831503052 |Tawanrat Prasong |
| 5 | 6831503098 |Nithit Panthavongdecha |
-----

## How to Run 

### Requirements 

- **Java JDK 17** (or newer ) — check with  `java -version`
- **Git** (to clone the repository )
- Internet on the first run (to download dependencies) 
- **Maven is optional** — the project includes the Maven Wrapper (`mvnw`, `mvnw.cmd`) 

### Step 1 — Clone the project 

```bash
git clone https://github.com/6831503039-cell/DormHub.git
```

### Step 2 — Run (choose ONE way) / รันโปรเจกต์ (เลือกวิธีใดวิธีหนึ่ง)

There are 2 ways to run it: Option A if Maven **is installed**, Option B if Maven is **not installed**. Both give the same result.

#### Option A — With Maven installed 

Check with `mvn -v`, then run :
```bash
mvn spring-boot:run
```

#### Option B — Without Maven (Maven Wrapper) 

The wrapper downloads Maven automatically on the first run. 
macOS / Linux:
```bash
./mvnw spring-boot:run
```
> If you get `Permission denied`, run `chmod +x mvnw` first. 

Windows (Command Prompt):
```bat
mvnw.cmd spring-boot:run
```

Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

### Step 3 — Open the website 

When the console shows `Started App`, open :

**http://localhost:8100**

- Port: **8100**
- To stop the server press `Ctrl + C`


When run locally with no extra settings, the app uses an **H2 in-memory database** and creates sample data (rooms, tenants, bills) automatically, so no database installation is needed (data is reset when the app stops).

> ⚠️ **AI Chatbot and Database**
>
> Running locally is for trying the general features only. For the **AI chatbot to work** and to use the **real database (MySQL on Google Cloud SQL)**, use the deployed website: https://dormhub-621353901508.us-central1.run.app

### Test Login 

Everyone logs in at the same page, `/login`. The owner is sent to `/admin` and tenants to `/app` automatically.

| Role | Email | Password |
|---|---|---|
| Admin (dorm owner) | `sjdadmin@gmail.com` | `dormhub123` |
| Tenant | `somchai@example.com` | `tenant123` |
| Tenant | `suda@example.com` | `tenant123` |
| Tenant | `manee@example.com` | `tenant123` |

These are demo accounts created by the sample data. 

> No passwords, API keys or tokens are committed in this repository. 

### Troubleshooting 

| Problem | Fix |
|---|---|
| `JAVA_HOME is not set` / `java: command not found` | Install JDK 17 and set `JAVA_HOME` 
| `Port 8100 was already in use` | Close the program using port 8100, or run with another port: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8200` /
| `mvn: command not found` | Maven is not installed — use Option B (`mvnw`)

---

## Tech Stack 

| Part | Technology |
|---|---|
| Backend | Java 17, Spring Boot 2.7, Spring MVC, Spring Data JPA (Hibernate), Spring Security |
| Database | H2 in-memory (local) / MySQL on Google Cloud SQL (production) |
| Front-end | Thymeleaf, Bootstrap 5, custom CSS, Lucide icons, Thai/English UI |
| AI | Google Gemini API (tenant chatbot, payment slip check) |
| Deployment | Docker, Google Cloud Build, Google Cloud Run, Cloud SQL |

## Main Features

**Dorm owner (`/admin`)**
- Dashboard: vacant rooms, income, unpaid bills, pending tasks 
- Rooms by building and floor with a floor plan 
- Tenants: assign to rooms, contract end dates 
- Monthly bills with water/electricity meter units; approve or reject slips 
- Booking requests, repairs, parcels (with photo), announcements, feedback 
- Analytics: income and unusual water/electricity usage 

**Tenant (`/app`)**
- View bills, pay with PromptPay QR, upload slip 
- Report repairs, send feedback, read announcements, confirm parcels 
- Laundry machine status 
- Water/electricity usage history 
- Chatbot for dorm questions 
- For people without a room yet: browse and book vacant rooms — /explore (no login needed) and /app/rent (registered account without a room) 
## Entities and Relationships (Spring Data JPA)

| Entity | Relationship |
|---|---|
| `Room` | `@OneToMany` → `Tenant`, `Bill`, `BookingRequest` |
| `Tenant` | `@ManyToOne` → `Room` |
| `Bill` | `@ManyToOne` → `Room`, `@OneToMany` → `Payment` |
| `Payment` | `@ManyToOne` → `Bill`, `Tenant` |
| `BookingRequest` | `@ManyToOne` → `Room`, `AppUser` |
| `RepairRequest` | `@ManyToOne` → `Room` |
| `Parcel` | `@ManyToOne` → `Room` |
| `Feedback` | `@ManyToOne` → `AppUser`, `Room` |
| `AppUser` | Login account (ADMIN / TENANT) |
| `Announcement`, `LaundryMachine` | Announcements and laundry machines |

## CRUD

| Function | Example |
|---|---|
| Listing / Searching | `/admin/rooms?q=...`, `/admin/tenants?q=...` (Spring Data JPA: `RoomRepository`, `TenantRepository`) with building/status filters |
| Creating | Add rooms, tenants, bills, repairs, parcels, announcements |
| Updating | Edit rooms, tenants, bills, repairs; approve slips; change booking status |
| Deleting | Delete rooms, tenants, bills, repairs, parcels, announcements |
| Related entities | Room for a tenant/bill/parcel, bill → payments, room → tenants |

## Project Structure 

```
pom.xml                Maven project file (root) 
mvnw, mvnw.cmd         Maven Wrapper (run without installing Maven)
src/main/java/th/mfu/
├── App.java           Main class (Spring Boot)
├── domain/            Entity classes (@Entity) and enums
├── *Repository.java   Spring Data JPA repositories
├── admin/             Owner controllers (/admin) / Controller 
├── app/               Tenant controllers (/app) / Controller 
├── explore/           Public vacant-room pages (/explore) 
├── service/           Business logic (bills, payments, chatbot, analytics, sample data)
├── security/          Login / Register (Spring Security)
└── config/            App configuration 
src/main/resources/
├── templates/         Thymeleaf pages 
├── static/            CSS, JavaScript, images 
├── application.properties
└── messages*.properties   Thai/English texts 
```

## Deployment (Google Cloud) 

1. Cloud Build reads `cloudbuild.yaml` and builds a Docker image from `Dockerfile` 
2. The image is pushed to Artifact Registry 
3. Deployed as the Cloud Run service `dormhub` (region `us-central1`, port 8100) 
4. Cloud Run connects to Cloud SQL (MySQL) using `DB_URL`, `DB_USER`, `DB_PASSWORD` set in Cloud Run, not in the code 
