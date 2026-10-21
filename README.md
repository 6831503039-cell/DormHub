# DormHub

ระบบจัดการหอพัก ใช้ Spring Boot + Thymeleaf + H2 และ deploy ขึ้น Google Cloud Run ผ่าน Cloud Build
(โครงสร้างต่อยอดจาก Lab Continuous Deployment)

## ฟีเจอร์

| เมนู | URL | ทำอะไรได้ |
|---|---|---|
| ภาพรวม | `/` | จำนวนห้อง/ห้องว่าง/ผู้เช่า/แจ้งซ่อมค้าง และบิลที่ยังไม่ชำระ |
| ห้องพัก | `/rooms` | เพิ่ม/แก้ไข/ลบห้อง กำหนดประเภท ค่าเช่า สถานะ |
| ผู้เช่า | `/tenants` | เพิ่ม/แก้ไข/ลบผู้เช่า ผูกกับห้อง (สถานะห้องเปลี่ยนเป็น "มีผู้เช่า"/"ว่าง" อัตโนมัติ) |
| บิลค่าเช่า | `/bills` | ออกบิลรายเดือน คิดค่าน้ำ/ค่าไฟตามหน่วย กดรับชำระ |
| แจ้งซ่อม | `/repairs` | บันทึกและติดตามสถานะการซ่อม |

## โครงสร้างโค้ด

```
src/main/java/th/mfu/
├── App.java                     จุดเริ่มโปรแกรม
├── domain/                      Entity (ตารางในฐานข้อมูล)
│   ├── Room.java, RoomStatus.java
│   ├── Tenant.java
│   ├── Bill.java
│   └── RepairRequest.java, RepairStatus.java
├── *Repository.java             ติดต่อฐานข้อมูล (Spring Data)
└── *Controller.java             รับ request และเลือกหน้าเว็บ
src/main/resources/
├── templates/                   หน้าเว็บ Thymeleaf + Bootstrap 5
├── data.sql                     ข้อมูลตัวอย่าง
└── application.properties
```

## รันในเครื่อง

ต้องมี JDK 17 ขึ้นไป และ Maven

```bash
mvn spring-boot:run
```

แล้วเปิด http://localhost:8100

หรือรันผ่าน Docker:

```bash
docker build -t dormhub .
docker run --rm -p 8100:8100 dormhub
```

## Deploy ขึ้น Cloud Run

ทำเหมือนใน Lab CD:

1. Cloud Build → Repositories → connect repo `DormHub`
2. Cloud Build → Triggers → Create Trigger ชื่อ `dormhub`, event `Push to a branch`, เลือก repo นี้
3. ใช้ Artifact Registry `webdev-dockers` ตัวเดิมได้ (ถ้าใช้ project อื่น ให้แก้ `_PROJECT_ID` ใน `cloudbuild.yaml`)
4. push โค้ด → Cloud Build จะ build และ deploy เป็น service ชื่อ `dormhub` ให้อัตโนมัติ

## หมายเหตุ

ฐานข้อมูลเป็น H2 แบบ in-memory ข้อมูลจะรีเซ็ตกลับเป็นข้อมูลตัวอย่างทุกครั้งที่ deploy ใหม่หรือ container รีสตาร์ท
ถ้าต้องการเก็บข้อมูลถาวร ให้เปลี่ยน `spring.datasource.*` ไปใช้ Cloud SQL (MySQL/PostgreSQL)
