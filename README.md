# DormHub — ระบบจัดการหอพัก SJD แมนชั่น

DormHub เป็นเว็บแอปจัดการหอพักที่ใช้ได้ทั้งเจ้าของหอและผู้เช่า
เจ้าของหอใช้จัดการห้อง ผู้เช่า บิลค่าเช่า ค่าน้ำ ค่าไฟ การชำระเงิน งานซ่อม พัสดุ และประกาศ ส่วนผู้เช่าใช้ดูบิล ส่งสลิป แจ้งซ่อม รับพัสดุ จองเครื่องซักผ้า และถามแชตบอตได้จากมือถือ

โปรเจกต์นี้เป็นงานร่วมของ 3 วิชา ได้แก่ Web Application Development, Database Systems และ Software Requirement Analysis and Specification

- **เว็บที่ deploy แล้ว (Google Cloud Run):** `<ใส่ลิงก์ Cloud Run>`
- **Repository:** https://github.com/6831503039-cell/DormHub

## Group Members

| # | รหัสนักศึกษา | ชื่อ - นามสกุล | หน้าที่ |
|---|---|---|---|
| 1 | 6831503039 | | |
| 2 | 6831503037 | | |
| 3 | 6831503098 | | |
| 4 | 6831503034 |Kanticha Sriruang | |
| 5 | | | |

## Tech Stack

| ส่วน | เทคโนโลยี |
|---|---|
| Backend | Java 17, **Spring Boot 2.7**, Spring MVC, **Spring Data JPA** (Hibernate), Spring Security |
| Database | **MySQL บน Google Cloud SQL** (production) / **H2 in-memory** (ตอนรันในเครื่อง) |
| Front-end | Thymeleaf, Bootstrap 5, CSS ของกลุ่มเอง (`static/css/app.css`), Lucide icons, ภาษาไทย/อังกฤษ |
| AI | Google Gemini API: แชตบอตผู้เช่าและตรวจสลิปโอนเงิน |
| Deployment | Docker, Google Cloud Build, **Google Cloud Run**, Cloud SQL |

## ฟีเจอร์หลัก

**ฝั่งเจ้าของหอ (`/admin`)**
- Dashboard สรุปห้องว่าง รายได้ บิลค้าง และงานที่รอดำเนินการ
- จัดการห้องพักแบบแยกตึก/ชั้น พร้อมแผนผังห้อง
- จัดการผู้เช่า ผูกผู้เช่ากับห้อง และดูวันหมดสัญญา
- ออกบิลรายเดือน คิดค่าน้ำ/ไฟตามหน่วย และอนุมัติหรือปฏิเสธสลิปที่ผู้เช่าส่งมา
- รับคำขอจองห้อง งานแจ้งซ่อม พัสดุ (ถ่ายรูปได้) ประกาศ และ feedback
- หน้า Analytics ดูรายได้และการใช้น้ำ/ไฟที่ผิดปกติ

**ฝั่งผู้เช่า (`/app`)**
- ดูบิลและจ่ายผ่าน PromptPay QR พร้อมอัปโหลดสลิป
- แจ้งซ่อม ส่ง feedback ดูประกาศ และกดรับพัสดุ
- ดูสถานะเครื่องซักผ้าและเริ่มใช้งาน
- ดูประวัติการใช้น้ำ/ไฟ
- แชตบอตตอบคำถามเรื่องหอ
- จองห้องว่าง (`/explore` และ `/app/rent`)

## Entity และความสัมพันธ์ (Spring Data JPA)

| Entity | ความสัมพันธ์ |
|---|---|
| `Room` | `@OneToMany` → `Tenant`, `Bill`, `BookingRequest` |
| `Tenant` | `@ManyToOne` → `Room` |
| `Bill` | `@ManyToOne` → `Room`, `@OneToMany` → `Payment` |
| `Payment` | `@ManyToOne` → `Bill`, `Tenant` |
| `BookingRequest` | `@ManyToOne` → `Room`, `AppUser` |
| `RepairRequest` | `@ManyToOne` → `Room` |
| `Parcel` | `@ManyToOne` → `Room` |
| `Feedback` | `@ManyToOne` → `AppUser`, `Room` |
| `AppUser` | บัญชีผู้ใช้สำหรับเข้าสู่ระบบ (ADMIN / TENANT) |
| `Announcement`, `LaundryMachine` | ประกาศของหอ และเครื่องซักผ้า |

## CRUD (Controls)

| ความสามารถ | ตัวอย่างในระบบ |
|---|---|
| Listing / Searching | `/admin/rooms?q=...` และ `/admin/tenants?q=...` ค้นหาผ่าน database ด้วย Spring Data JPA (`RoomRepository`, `TenantRepository`) และมีตัวกรองตึก/สถานะ |
| Creating | เพิ่มห้อง ผู้เช่า บิล งานซ่อม พัสดุ และประกาศ |
| Updating | แก้ไขข้อมูลห้อง ผู้เช่า บิล และงานซ่อม, อนุมัติสลิป, เปลี่ยนสถานะการจอง |
| Deleting | ลบห้อง ผู้เช่า บิล งานซ่อม พัสดุ และประกาศ |
| Managing related entities | เลือกห้องให้ผู้เช่า/บิล/พัสดุ, บิล → การชำระเงิน, ห้อง → รายชื่อผู้เช่า |

## วิธีรันในเครื่อง

ต้องมี Java 17 (Maven Wrapper มากับโปรเจกต์แล้ว)

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

เปิด http://localhost:8100 ถ้าไม่ได้ตั้ง `DB_URL` ระบบจะใช้ H2 in-memory และสร้างข้อมูลตัวอย่างให้ (`DataSeeder`)

### Environment variables

| ตัวแปร | ใช้ทำอะไร |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | เชื่อม MySQL บน Cloud SQL (ไม่ตั้ง = ใช้ H2) |
| `GEMINI_API_KEY` | เปิดใช้แชตบอตและการตรวจสลิปด้วย AI |

ห้าม commit รหัสผ่าน database หรือ API key ลง repository

## การ Deploy บน Google Cloud

1. Cloud Build อ่าน `cloudbuild.yaml` แล้ว build Docker image ตาม `Dockerfile`
2. Push image ขึ้น Artifact Registry (`us-central1-docker.pkg.dev/dormhub-512309/dormhub-dockers`)
3. Deploy เป็น Cloud Run service ชื่อ `dormhub` (region `us-central1`, port 8100)
4. Cloud Run เชื่อม Cloud SQL (MySQL) ผ่าน Cloud SQL Socket Factory โดยตั้ง `DB_URL` เป็น
   `jdbc:mysql:///dormhub?cloudSqlInstance=dormhub-512309:us-central1:free-trial-first-dormhub&socketFactory=com.google.cloud.sql.mysql.SocketFactory`

## บัญชีสำหรับทดสอบ

ทุกคนเข้าสู่ระบบที่ `/login` หน้าเดียว ระบบจะพาเจ้าของหอไปที่ `/admin` และพาผู้เช่าไปที่ `/app` ให้เอง

| บัญชี | อีเมล | รหัสผ่าน | หมายเหตุ |
|---|---|---|---|
| แอดมินหอ | `sjdadmin@gmail.com` | `dormhub123` | ล็อกไว้เป็นแอดมินเท่านั้น สมัครด้วยอีเมลนี้ไม่ได้ |
| ผู้เช่าตัวอย่าง | `somchai@example.com`, `suda@example.com`, `manee@example.com` | `tenant123` | ข้อมูลตัวอย่าง |

## โครงสร้างโปรเจกต์

```
src/main/java/th/mfu/
├── domain/     Entity classes (@Entity) และ enum
├── *Repository.java   Spring Data JPA repositories
├── admin/      Controller ฝั่งเจ้าของหอ (/admin)
├── app/        Controller ฝั่งผู้เช่า (/app)
├── explore/    หน้าสาธารณะสำหรับดูห้องว่าง (/explore)
├── service/    Business logic (บิล, การชำระเงิน, แชตบอต, analytics)
├── security/   Login / Register (Spring Security)
└── config/     การตั้งค่าแอป
src/main/resources/
├── templates/  หน้าเว็บ Thymeleaf
├── static/     CSS, JavaScript, รูปภาพ
└── messages*.properties   ข้อความภาษาไทย/อังกฤษ
```
