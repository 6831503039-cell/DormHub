# DormHub — ระบบจัดการหอพัก (Dormitory Management System)

**TH:** DormHub เป็นเว็บแอปจัดการหอพัก (SJD แมนชั่น) ที่ใช้ได้ทั้งเจ้าของหอและผู้เช่า เจ้าของหอใช้จัดการห้องพัก ผู้เช่า บิลค่าเช่า ค่าน้ำ ค่าไฟ การชำระเงิน งานแจ้งซ่อม พัสดุ และประกาศ ส่วนผู้เช่าใช้ดูบิลและจ่ายเงินผ่าน PromptPay พร้อมอัปโหลดสลิป แจ้งซ่อม รับพัสดุ จองเครื่องซักผ้า จองห้องว่าง และถามคำถามกับแชตบอต พัฒนาด้วย Spring Boot, Spring Data JPA และ Thymeleaf เป็นโปรเจกต์ร่วมของวิชา Web Application Development, Database Systems และ Software Requirement Analysis and Specification

**EN:** DormHub is a web application for managing a dormitory (SJD Mansion), used by both the dorm owner and the tenants. The owner manages rooms, tenants, monthly bills (rent, water and electricity), payments, repair requests, parcels and announcements. Tenants view and pay their bills with a PromptPay QR code and upload the payment slip, report repairs, pick up parcels, use the laundry machines, book a vacant room and ask questions to a chatbot. It is built with Spring Boot, Spring Data JPA and Thymeleaf as a joint project for Web Application Development, Database Systems, and Software Requirement Analysis and Specification.

- **Live demo (Google Cloud Run):** https://dormhub-621353901508.us-central1.run.app
- **Repository:** https://github.com/6831503039-cell/DormHub

---

## Group Members / สมาชิกกลุ่ม

```
6831503034  Kanticha Sriruang
6831503037  Chawintorn Thanapreechanan
6831503039  Yanavut Thidnongwaeng
6831503052  Tawanrat Prasong
6831503098  Nithit Panthavongdecha
```

---

## How to Run / วิธีรันโปรเจกต์

### Requirements / สิ่งที่ต้องมี

- **Java JDK 17** (or newer / หรือใหม่กว่า) — check with / ตรวจสอบด้วย `java -version`
- **Git** (to clone the repository / สำหรับ clone โปรเจกต์)
- Internet on the first run (to download dependencies) / ต้องต่ออินเทอร์เน็ตตอนรันครั้งแรก เพื่อดาวน์โหลด dependencies
- **Maven is optional** — the project includes the Maven Wrapper (`mvnw`, `mvnw.cmd`) / **ไม่ต้องลง Maven ก็ได้** เพราะโปรเจกต์มี Maven Wrapper มาให้แล้ว

### Step 1 — Clone the project / ดาวน์โหลดโปรเจกต์

```bash
git clone https://github.com/6831503039-cell/DormHub.git
cd DormHub
```

### Step 2 — Run (choose ONE way) / รันโปรเจกต์ (เลือกวิธีใดวิธีหนึ่ง)

**TH:** รันได้ 2 แบบ แบบที่ 1 สำหรับเครื่องที่**ไม่ได้ลง Maven** และแบบที่ 2 สำหรับเครื่องที่**ลง Maven แล้ว** ทั้งสองแบบได้ผลเหมือนกัน

**EN:** There are 2 ways to run it: Option A if Maven is **not installed**, Option B if Maven **is installed**. Both give the same result.

#### Option A — Without Maven (Maven Wrapper) / แบบที่ 1: ไม่ได้ลง Maven

The wrapper downloads Maven automatically on the first run. / Wrapper จะดาวน์โหลด Maven ให้อัตโนมัติในการรันครั้งแรก

macOS / Linux:
```bash
./mvnw spring-boot:run
```
> If you get `Permission denied`, run `chmod +x mvnw` first. / ถ้าขึ้น `Permission denied` ให้รัน `chmod +x mvnw` ก่อน

Windows (Command Prompt):
```bat
mvnw.cmd spring-boot:run
```

Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

#### Option B — With Maven installed / แบบที่ 2: ลง Maven แล้ว

Check with / ตรวจสอบด้วย `mvn -v`, then run / จากนั้นรัน:
```bash
mvn spring-boot:run
```

### Step 3 — Open the website / เปิดเว็บ

When the console shows `Started App`, open / เมื่อหน้าจอขึ้นว่า `Started App` ให้เปิด:

**http://localhost:8100**

- Port: **8100**
- To stop the server press / หยุดเซิร์ฟเวอร์ด้วย `Ctrl + C`

**TH:** เมื่อรันในเครื่องโดยไม่ได้ตั้งค่าอะไรเพิ่ม ระบบจะใช้ฐานข้อมูล **H2 in-memory** และสร้างข้อมูลตัวอย่าง (ห้อง ผู้เช่า บิล) ให้อัตโนมัติ ไม่ต้องติดตั้งฐานข้อมูลเอง (ข้อมูลจะหายเมื่อปิดโปรแกรม)

**EN:** When run locally with no extra settings, the app uses an **H2 in-memory database** and creates sample data (rooms, tenants, bills) automatically, so no database installation is needed (data is reset when the app stops).

> ⚠️ **AI Chatbot / แชตบอต AI**
>
> **TH:** ถ้ารันในเครื่องตัวเอง **ต้องใส่ Gemini API key** (environment variable `GEMINI_API_KEY`) ก่อนรัน ไม่อย่างนั้น**แชตบอตและการตรวจสลิปด้วย AI จะใช้งานไม่ได้** (ฟีเจอร์อื่นยังใช้ได้ปกติ) ถ้าต้องการทดลองแชตบอตโดยไม่ต้องใส่ key ให้ใช้เว็บที่ deploy แล้ว: https://dormhub-621353901508.us-central1.run.app
>
> **EN:** When running locally you **must provide a Gemini API key** (environment variable `GEMINI_API_KEY`) before starting, otherwise **the chatbot and the AI slip check will not work** (all other features still work). To try the chatbot without a key, use the deployed site: https://dormhub-621353901508.us-central1.run.app
>
> Set the key, then run / ตั้งค่า key แล้วค่อยรัน (replace `your-key` with your own key / เปลี่ยน `your-key` เป็น key ของตัวเอง):
>
> ```bash
> # macOS / Linux
> export GEMINI_API_KEY=your-key
> ./mvnw spring-boot:run
> ```
> ```bat
> :: Windows (Command Prompt)
> set GEMINI_API_KEY=your-key
> mvnw.cmd spring-boot:run
> ```
> ```powershell
> # Windows (PowerShell)
> $env:GEMINI_API_KEY="your-key"
> .\mvnw.cmd spring-boot:run
> ```
>
> Get a free key at / ขอ key ได้ฟรีที่ https://aistudio.google.com/apikey — never commit the key to the repository / ห้าม commit key ลง repository

### Test Login / บัญชีสำหรับทดสอบ

**TH:** ทุกคนเข้าสู่ระบบที่หน้า `/login` หน้าเดียว ระบบจะพาเจ้าของหอไปที่ `/admin` และผู้เช่าไปที่ `/app` ให้เอง

**EN:** Everyone logs in at the same page, `/login`. The owner is sent to `/admin` and tenants to `/app` automatically.

| Role / บทบาท | Email / อีเมล | Password / รหัสผ่าน |
|---|---|---|
| Admin (dorm owner) / แอดมิน (เจ้าของหอ) | `sjdadmin@gmail.com` | `dormhub123` |
| Tenant / ผู้เช่า | `somchai@example.com` | `tenant123` |
| Tenant / ผู้เช่า | `suda@example.com` | `tenant123` |
| Tenant / ผู้เช่า | `manee@example.com` | `tenant123` |

These are demo accounts created by the sample data. / บัญชีเหล่านี้เป็นบัญชีตัวอย่างที่ระบบสร้างให้ตอนเริ่มโปรแกรม

### Optional settings / การตั้งค่าเพิ่มเติม (ไม่บังคับ)

**TH:** ไม่ตั้งค่าก็เปิดเว็บได้ แต่ต้องตั้ง `GEMINI_API_KEY` ถ้าจะใช้แชตบอต AI ส่วน `DB_*` ตั้งเฉพาะเมื่อต้องการใช้ MySQL

**EN:** The app starts without these. `GEMINI_API_KEY` is needed for the AI chatbot; the `DB_*` variables only if you want MySQL.

| Variable / ตัวแปร | Purpose / ใช้ทำอะไร |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connect to MySQL instead of H2 / เชื่อม MySQL แทน H2 |
| `GEMINI_API_KEY` | Enable the AI chatbot and AI slip check / เปิดใช้แชตบอตและการตรวจสลิปด้วย AI |

Without `GEMINI_API_KEY` the chatbot does not work locally (see the AI Chatbot note above); every other feature still works. / ถ้าไม่ได้ตั้ง `GEMINI_API_KEY` แชตบอตจะใช้ไม่ได้ในเครื่อง (ดูหมายเหตุแชตบอต AI ด้านบน) แต่ฟีเจอร์อื่นยังใช้งานได้ตามปกติ

> No passwords, API keys or tokens are committed in this repository; they are passed as environment variables. / ไม่มีรหัสผ่าน database, API key หรือ token อยู่ใน repository ค่าเหล่านี้ส่งผ่าน environment variables เท่านั้น

### Troubleshooting / แก้ปัญหาเบื้องต้น

| Problem / ปัญหา | Fix / วิธีแก้ |
|---|---|
| `JAVA_HOME is not set` / `java: command not found` | Install JDK 17 and set `JAVA_HOME` / ติดตั้ง JDK 17 และตั้งค่า `JAVA_HOME` |
| `Port 8100 was already in use` | Close the program using port 8100, or run with another port: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8200` / ปิดโปรแกรมที่ใช้พอร์ต 8100 หรือรันด้วยพอร์ตอื่นตามคำสั่งนี้ |
| `mvn: command not found` | Maven is not installed — use Option A (`mvnw`) / ยังไม่ได้ลง Maven ให้ใช้แบบที่ 1 (`mvnw`) |

---

## Tech Stack / เทคโนโลยีที่ใช้

| Part / ส่วน | Technology / เทคโนโลยี |
|---|---|
| Backend | Java 17, Spring Boot 2.7, Spring MVC, Spring Data JPA (Hibernate), Spring Security |
| Database | H2 in-memory (local) / MySQL on Google Cloud SQL (production) |
| Front-end | Thymeleaf, Bootstrap 5, custom CSS, Lucide icons, Thai/English UI |
| AI | Google Gemini API (tenant chatbot, payment slip check) |
| Deployment | Docker, Google Cloud Build, Google Cloud Run, Cloud SQL |

## Main Features / ฟีเจอร์หลัก

**Dorm owner / เจ้าของหอ (`/admin`)**
- Dashboard: vacant rooms, income, unpaid bills, pending tasks / สรุปห้องว่าง รายได้ บิลค้าง งานที่รอดำเนินการ
- Rooms by building and floor with a floor plan / จัดการห้องแยกตึก/ชั้น พร้อมแผนผังห้อง
- Tenants: assign to rooms, contract end dates / จัดการผู้เช่า ผูกกับห้อง ดูวันหมดสัญญา
- Monthly bills with water/electricity meter units; approve or reject slips / ออกบิลรายเดือน คิดค่าน้ำ/ไฟตามหน่วย อนุมัติหรือปฏิเสธสลิป
- Booking requests, repairs, parcels (with photo), announcements, feedback / คำขอจองห้อง งานซ่อม พัสดุ (ถ่ายรูปได้) ประกาศ feedback
- Analytics: income and unusual water/electricity usage / ดูรายได้และการใช้น้ำ/ไฟที่ผิดปกติ

**Tenant / ผู้เช่า (`/app`)**
- View bills, pay with PromptPay QR, upload slip / ดูบิล จ่ายผ่าน PromptPay QR และอัปโหลดสลิป
- Report repairs, send feedback, read announcements, confirm parcels / แจ้งซ่อม ส่ง feedback ดูประกาศ กดรับพัสดุ
- Laundry machine status / ดูสถานะและเริ่มใช้เครื่องซักผ้า
- Water/electricity usage history / ดูประวัติการใช้น้ำ/ไฟ
- Chatbot for dorm questions / แชตบอตตอบคำถามเรื่องหอ
- Browse and book vacant rooms (`/explore`, `/app/rent`) / ดูและจองห้องว่าง

## Entities and Relationships / Entity และความสัมพันธ์ (Spring Data JPA)

| Entity | Relationship / ความสัมพันธ์ |
|---|---|
| `Room` | `@OneToMany` → `Tenant`, `Bill`, `BookingRequest` |
| `Tenant` | `@ManyToOne` → `Room` |
| `Bill` | `@ManyToOne` → `Room`, `@OneToMany` → `Payment` |
| `Payment` | `@ManyToOne` → `Bill`, `Tenant` |
| `BookingRequest` | `@ManyToOne` → `Room`, `AppUser` |
| `RepairRequest` | `@ManyToOne` → `Room` |
| `Parcel` | `@ManyToOne` → `Room` |
| `Feedback` | `@ManyToOne` → `AppUser`, `Room` |
| `AppUser` | Login account (ADMIN / TENANT) / บัญชีผู้ใช้สำหรับเข้าสู่ระบบ |
| `Announcement`, `LaundryMachine` | Announcements and laundry machines / ประกาศและเครื่องซักผ้า |

## CRUD

| Function / ความสามารถ | Example / ตัวอย่างในระบบ |
|---|---|
| Listing / Searching | `/admin/rooms?q=...`, `/admin/tenants?q=...` (Spring Data JPA: `RoomRepository`, `TenantRepository`) with building/status filters / พร้อมตัวกรองตึกและสถานะ |
| Creating | Add rooms, tenants, bills, repairs, parcels, announcements / เพิ่มห้อง ผู้เช่า บิล งานซ่อม พัสดุ ประกาศ |
| Updating | Edit rooms, tenants, bills, repairs; approve slips; change booking status / แก้ไขข้อมูล อนุมัติสลิป เปลี่ยนสถานะการจอง |
| Deleting | Delete rooms, tenants, bills, repairs, parcels, announcements / ลบข้อมูลต่าง ๆ |
| Related entities | Room for a tenant/bill/parcel, bill → payments, room → tenants / เลือกห้องให้ผู้เช่า/บิล/พัสดุ, บิล → การชำระเงิน, ห้อง → ผู้เช่า |

## Project Structure / โครงสร้างโปรเจกต์

```
pom.xml                Maven project file (root) / ไฟล์ Maven
mvnw, mvnw.cmd         Maven Wrapper (run without installing Maven) / รันได้โดยไม่ต้องลง Maven
src/main/java/th/mfu/
├── App.java           Main class (Spring Boot)
├── domain/            Entity classes (@Entity) and enums
├── *Repository.java   Spring Data JPA repositories
├── admin/             Owner controllers (/admin) / Controller ฝั่งเจ้าของหอ
├── app/               Tenant controllers (/app) / Controller ฝั่งผู้เช่า
├── explore/           Public vacant-room pages (/explore) / หน้าดูห้องว่าง
├── service/           Business logic (bills, payments, chatbot, analytics, sample data)
├── security/          Login / Register (Spring Security)
└── config/            App configuration / การตั้งค่าแอป
src/main/resources/
├── templates/         Thymeleaf pages / หน้าเว็บ
├── static/            CSS, JavaScript, images / รูปภาพ
├── application.properties
└── messages*.properties   Thai/English texts / ข้อความภาษาไทย/อังกฤษ
```

## Deployment (Google Cloud) / การ Deploy

1. Cloud Build reads `cloudbuild.yaml` and builds a Docker image from `Dockerfile` / Cloud Build อ่าน `cloudbuild.yaml` แล้ว build Docker image จาก `Dockerfile`
2. The image is pushed to Artifact Registry / Push image ขึ้น Artifact Registry
3. Deployed as the Cloud Run service `dormhub` (region `us-central1`, port 8100) / Deploy เป็น Cloud Run service ชื่อ `dormhub`
4. Cloud Run connects to Cloud SQL (MySQL) using `DB_URL`, `DB_USER`, `DB_PASSWORD` set in Cloud Run, not in the code / Cloud Run เชื่อม Cloud SQL ผ่าน environment variables ที่ตั้งใน Cloud Run ไม่ได้อยู่ในโค้ด
