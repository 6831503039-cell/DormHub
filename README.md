# DormHub

ต้องเอาไปรันใน Vs code นะ Jdk version 17 ขึ้นไป

วิธีรัน:

ถ้าไม่มี Maven ในเครื่อง พิมพ์ใน terminal .\mvnw.cmd spring-boot:run
ถ้ามี Maven ในเครื่อง พิมพ์ใน terminal mvn spring-boot:run








ระบบจัดการหอพักนักศึกษา: Web App สำหรับเจ้าของหอ และแอป (มือถือ) สำหรับผู้เช่า
Spring Boot + Thymeleaf + Spring Security + H2, deploy ขึ้น Google Cloud Run ผ่าน Cloud Build

## เข้าสู่ระบบ (Login รวมศูนย์)

หน้า `/login` หน้าเดียว ระบบแยกสิทธิ์ให้อัตโนมัติ: เจ้าของหอไปที่ Dashboard (`/admin`) ผู้เช่าไปที่แอป (`/app`)

| บัญชี | อีเมล | รหัสผ่าน | หมายเหตุ |
|---|---|---|---|
| แอดมินหอ | `sjdadmin@gmail.com` | `dormhub123` | ล็อกไว้เป็นแอดมินเท่านั้น สมัครด้วยอีเมลนี้ไม่ได้ |
| ผู้เช่าตัวอย่าง | `somchai@example.com`, `suda@example.com`, `manee@example.com` | `tenant123` | ข้อมูลตัวอย่าง |

- ใครสมัครที่ `/register` ด้วยอีเมลอื่น จะเป็น **ผู้เช่า** เสมอ
- บัญชีผู้เช่าเชื่อมกับห้องผ่าน **อีเมล**: ถ้าอีเมลที่สมัครตรงกับอีเมลในข้อมูลผู้เช่าที่แอดมินบันทึก จะเห็นห้อง บิล และเพื่อนร่วมห้องของตัวเอง
- เปลี่ยนอีเมล/รหัสแอดมินได้ด้วย environment variable `DORMHUB_ADMIN_EMAIL` / `DORMHUB_ADMIN_PASSWORD` (แนะนำให้ตั้งบน Cloud Run เพราะ repo นี้เป็น public)

## ฟีเจอร์

**เจ้าของหอ (Web App)**
- Dashboard: ห้องว่าง, ผู้เช่า, สลิปรอตรวจ, แจ้งซ่อมค้าง, ฟีดแบ็กจากผู้เช่า, ค่าไฟผิดปกติ, สถานะเครื่องซักผ้า
- แผนผังห้อง (Room Layout) แยกตามชั้น ดูสถานะว่าง/มีผู้เช่า/ปิดซ่อม
- จัดการห้อง (กำหนดความจุ 1-3 คน), ผู้เช่า, บิล/จดมิเตอร์
- วิเคราะห์ค่าไฟ: กราฟรายเดือน 12 เดือน และค่าเฉลี่ยตาม 3 ฤดู (ร้อน/ฝน/หนาว)
- ตรวจสลิป: ดูรูปสลิป ผลตรวจจาก AI และกดอนุมัติ/ไม่อนุมัติ

**แผนผังหอพัก 3D (หน้าจองห้อง)**
- หน้า `/explore` เปิดได้โดยไม่ต้องล็อกอิน: โมเดล 3 มิติของหอ SJD แมนชั่นตามของจริง หมุน/ซูมได้
  - ตึก A (กลาง, 80 ห้อง) ตึก B (ขวา, 70 ห้อง) ตึก C (ซ้าย, 70 ห้อง) สูง 4 ชั้น บันไดกลางตึก ห้องเรียง 1-2-2-[บันได]-2-2-1 ระเบียงคู่ราวสแตนเลส
  - ป้าย SJD แมนชั่นบนหอบันไดตึก A, ทางเข้าตึกเสากลม, ห้องชั้น 1 มีประตูออกนอกพร้อมบันไดและระแนง
  - ชั้น 1 ของตึก B/C ครึ่งหนึ่งเป็นที่จอดรถใต้ตึก (ตึก C มีสำนักงานและที่ซักผ้า), ลานส่วนกลางมีโต๊ะหิน, ลานจอดรถ, ทางเข้าหอ, ศาลพระภูมิ, โรงจอดมอเตอร์ไซค์หลังตึก
- กดตึก → ห้องทุกห้องเปลี่ยนสีตามสถานะ: ว่าง / ใกล้หมดสัญญา (เหลือ ≤ 60 วัน คิดจากวันหมดสัญญาของผู้เช่า) / มีผู้เช่า / ปิดซ่อม
- เลือกชั้น → ชั้นด้านบนค่อยๆ ยกออก เห็นห้องในชั้นนั้น กดห้องดูราคา รายละเอียด และ **ห้องแบบ 3D** ที่ทำตามรูปห้องจริง (ผนังสีส้ม/เขียว เตียงหัวขาวที่นอนหนังดำ ตู้เสื้อผ้ากระจก โต๊ะยาว+ทีวี ตู้เย็น แอร์ ประตูกระจกเลื่อน ระเบียง ห้องน้ำ; จำนวนเตียงตามจำนวนคน)
- ส่ง **คำขอจอง** (ห้องใกล้หมดสัญญาจองล่วงหน้าได้) → แอดมินเห็นที่เมนู "คำขอจอง" และกดยืนยัน/ยกเลิก
- หน้า "แผนผังห้อง" ของแอดมินใช้โมเดล 3D เดียวกัน แต่แสดงชื่อผู้เช่าและลิงก์ไปแก้ไขห้อง
- โค้ด: `static/js/dorm3d.js` (Three.js r128 จาก CDN), ข้อมูลจาก `RoomMapService` ผ่าน `/explore/rooms` และ `/admin/layout/rooms` (JSON)

**ผู้เช่า (แอป)**
- หน้าแรก: ยอดที่ต้องจ่าย (ส่วนของตัวเอง), การแจ้งเตือน, เพื่อนร่วมห้อง
- บิล: **หารค่าห้องอัตโนมัติ** ตามจำนวนคนในห้อง, **QR พร้อมเพย์** สแกนจ่ายตามยอด, อัปโหลดสลิป
- **AI ตรวจสลิป** (Gemini): ยอดตรงและไม่พบการตัดต่อ จะตัดยอดอัตโนมัติ ถ้าสงสัยจะส่งให้แอดมินตรวจ
- **Smart Virtual Admin (AI Chatbot)** ด้วย Gemini: ตอบเรื่อง Wi-Fi, ยอดค้าง, เครื่องซักผ้า, กฎหอ, การแจ้งซ่อม
- การแจ้งเตือน: ชำระเงินล่วงหน้า 5 วันก่อนครบกำหนด / เลยกำหนด, ค่าไฟล่วงหน้า 7 วันก่อนสิ้นเดือน (ประมาณการ), ค่าไฟสูงผิดปกติ (>30% จากค่าเฉลี่ย 3 เดือน)
- ห้องซักผ้า: ดูเครื่องว่าง/ไม่ว่าง และกดเริ่มใช้งาน (**Mockup จำลอง** ไม่ได้ต่อเครื่องจริง)
- คำร้อง: แจ้งซ่อม และส่งความคิดเห็นถึงเจ้าของหอ

## ตั้งค่า Gemini API (AI Chatbot + ตรวจสลิป)

1. สร้าง API key ที่ https://aistudio.google.com/apikey
2. ตั้ง environment variable ชื่อ `GEMINI_API_KEY` (ห้ามใส่ key ลงในโค้ดหรือ commit ขึ้น GitHub)
   - ในเครื่อง (PowerShell): `$env:GEMINI_API_KEY = 'คีย์ของคุณ'` แล้วค่อยรันแอปใน terminal เดียวกัน
   - บน Cloud Run: เปิด service `dormhub` → Edit & deploy new revision → Variables & Secrets → เพิ่ม `GEMINI_API_KEY`
3. เปลี่ยนรุ่นโมเดลได้ด้วย `GEMINI_MODEL` (ค่าเริ่มต้น `gemini-2.5-flash`)

ถ้าไม่ได้ตั้ง key แอปยังใช้งานได้ปกติ: แชทบอทตอบจากชุดคำตอบสำเร็จรูป และสลิปจะรอแอดมินตรวจเอง

> AI ช่วยคัดกรองสลิปเบื้องต้นได้ แต่ไม่สามารถยืนยันว่าเงินเข้าบัญชีจริง ถ้าต้องการความแม่นยำระดับใช้งานจริง ควรต่อ API ตรวจสลิปของธนาคารเพิ่ม

## รันในเครื่อง

ต้องมี JDK 17 ขึ้นไป (ไม่ต้องลง Maven ใช้ Maven Wrapper ที่มากับโปรเจกต์)

```bash
.\mvnw.cmd spring-boot:run
```

เปิด http://localhost:8100 (Mac/Linux ใช้ `./mvnw spring-boot:run`) รัน test ด้วย `.\mvnw.cmd test`

## โครงสร้างโค้ด

```
src/main/java/th/mfu/
├── domain/      Entity: Room, Tenant, Bill, Payment, Feedback, RepairRequest, LaundryMachine, AppUser
├── security/    SecurityConfig, login/register (AuthController)
├── service/     BillingService (หารค่าห้อง/ตัดยอด), NotificationService, AnalyticsService,
│                GeminiClient, ChatbotService, SlipVerificationService, PromptPay, DataSeeder
├── admin/       Controller ฝั่งเจ้าของหอ (/admin/**)
├── explore/     ExploreController หน้าแผนผัง 3D และคำขอจองสำหรับคนทั่วไป (/explore)
├── app/         Controller ฝั่งผู้เช่า (/app/**)
└── *Repository.java
src/main/resources/
├── templates/fragments/  layout ของ admin และ app
├── templates/admin, app, auth
├── static/css/app.css    ธีมสีฟ้า minimal
└── static/js/dorm3d.js   แผนผังหอพัก 3 มิติ + ห้อง 3D
```

## Deploy ขึ้น Cloud Run

1. Cloud Build → Repositories → connect repo `DormHub`
2. Cloud Build → Triggers → Create Trigger ชื่อ `dormhub`, event `Push to a branch`
3. push โค้ด → Cloud Build จะ build และ deploy เป็น service `dormhub` อัตโนมัติ
4. ตั้ง `GEMINI_API_KEY` (และแนะนำ `DORMHUB_ADMIN_PASSWORD`) ใน Variables ของ service

## หมายเหตุ

ฐานข้อมูลเป็น H2 แบบ in-memory: ข้อมูลและบัญชีที่สมัครใหม่จะรีเซ็ตเป็นข้อมูลตัวอย่างทุกครั้งที่ deploy ใหม่หรือ container รีสตาร์ท
ถ้าต้องการเก็บข้อมูลถาวร ให้เปลี่ยน `spring.datasource.*` ไปใช้ Cloud SQL (MySQL/PostgreSQL)
