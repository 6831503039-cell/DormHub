# DormHub Design System (Master)

ทิศทาง: **minimal ใช้งานง่าย สะอาด มืออาชีพ** ใช้บนมือถือเป็นหลัก (เจ้าของหอและผู้เช่า)
ทุกค่าเก็บเป็น token ที่เดียวใน `src/main/resources/static/css/app.css` (`:root`) ห้ามเขียนค่าสี/ขนาดตรงๆ ใน template

## หลักการ
1. ข้อมูลสำคัญก่อน: ห้องว่าง, ยอดค้างชำระ, งานซ่อมที่รอ ต้องเห็นได้โดยไม่ต้องเลื่อน
2. Neutral เป็นหลัก สีหลักใช้กับการกระทำ (ปุ่มหลัก ลิงก์ เมนูที่เลือก) สีสถานะใช้กับสถานะเท่านั้น
3. ไม่มี gradient, emoji, glassmorphism ไม่มีการ์ดซ้อนการ์ด ใช้หัวข้อ + เส้นบาง (hairline) + ระยะห่าง แทนกรอบ เงาใช้เฉพาะของที่ลอย (dropdown, modal)
4. ทุกปุ่ม/ลิงก์ที่กดได้บนมือถือ สูงอย่างน้อย 44px
5. Contrast ข้อความอย่างน้อย 4.5:1 ทุกคู่สี

## สี

ธีมมาจากโลโก้ DormHub: **เขียวเสจ** (เส้นบ้าน), **ทราย** (เตียง หน้าต่าง หมุด), **เทาอมฟ้า** (ตัวอักษร), **ครีม** (พื้น)
เขียวเสจในโลโก้ (`#8faa8b`) อ่อนเกินสำหรับปุ่มตัวหนังสือขาว จึงใช้เฉดเข้ม `#4f7a58` เป็นสีหลัก

| Token | ค่า | ใช้กับ |
|---|---|---|
| `--c-bg` | `#f7f6f1` | พื้นหลังหน้า (ครีม) |
| `--c-surface` | `#ffffff` | การ์ด ตาราง ช่องกรอก |
| `--c-surface-2` | `#faf9f5` | หัวตาราง |
| `--c-hover` | `#f1efe8` | hover แถว/รายการ |
| `--c-border` / `--c-border-strong` | `#e6e3d8` / `#d3cfc2` | เส้นขอบ / ขอบช่องกรอก |
| `--c-text` | `#1f2522` | ข้อความหลัก (15.6:1) |
| `--c-text-2` | `#4a524e` | ข้อความรอง, label (8:1) |
| `--c-muted` | `#5f6a66` | คำอธิบาย, caption (5.6:1 บนขาว, 5.2:1 บนพื้น) |
| `--c-primary` | `#4f7a58` | ปุ่มหลัก ลิงก์ (ตัวขาวบนสีนี้ 4.9:1) |
| `--c-primary-soft` / `--c-primary-text` | `#edf3ec` / `#3d6346` | เมนูที่เลือก ปุ่มรอง (6:1) |
| `--c-accent` / `--c-accent-soft` | `#c9a66b` / `#f6efe1` | สีทรายจากโลโก้: ดาวคะแนน จุดเน้น **ห้ามใช้เป็นตัวหนังสือ** |

### สีสถานะ (dot = จุด/แถบสี, fg/bg = badge)

สีหลักเป็นเขียวแล้ว "มีผู้เช่า" จึงใช้สีเทาอมฟ้าจากตัวอักษรในโลโก้ แทนสีหลัก เพื่อไม่ให้ปนกับ "ว่าง" (เขียวสด)

| สถานะ | Token | dot | fg / bg | ใช้กับ |
|---|---|---|---|---|
| success | `--s-success*` | `#2e9a5a` | `#1f7a45` / `#e6f4ea` | ว่าง, ชำระแล้ว, ซ่อมเสร็จ, อนุมัติ |
| info | `--s-info*` | `#5b7a9d` | `#40597a` / `#ecf0f5` | มีผู้เช่า, ฤดูฝน (กราฟ) |
| warning | `--s-warning*` | `#d99a00` | `#8a5a12` / `#f8efdc` | รอซ่อม, ปิดซ่อม, สลิปรอตรวจ, ฤดูร้อน (กราฟ) |
| soon | `--s-soon*` | `#e8833a` | `#a6450c` / `#fdf0e6` | ใกล้หมดสัญญา |
| danger | `--s-danger*` | `#d92d20` | `#b42318` / `#fdecea` | ค้างชำระ, เลยกำหนด, สลิปน่าสงสัย |
| neutral | `--s-neutral*` | `#a3a8a2` | `#4a524e` / `#efeee8` | ไม่อนุมัติ, อื่นๆ, ฤดูหนาว (กราฟ) |

สีสถานะในแผนผัง 3D (`static/js/dorm3d.js` → `COLORS`, `CSS_COLORS`) ต้องตรงกับค่า dot ในตารางนี้

ป้ายสถานะใช้ fragment `ui :: status(tone, text)` (ดูหัวข้อ "ป้ายสถานะ") class badge เดิม (`soft-*`, `status-*`) ยังแสดงผลเป็นแบบจุดเหมือนกัน แต่ไม่ต้องใช้ในหน้าใหม่

## ตัวอักษร
รองรับทั้งภาษาไทยและอังกฤษ (เปลี่ยนภาษาได้ในหน้าตั้งค่า) โหลดทั้ง 3 ฟอนต์จาก Google Fonts ครั้งเดียวที่ `fragments/fonts.html` (`~{fragments/fonts :: fonts}` ในทุก `<head>`, preconnect + `display=swap`)

| Token | ฟอนต์ | ใช้กับ |
|---|---|---|
| `--font` | **Geist** (อังกฤษ) แล้วตามด้วย **Anuphan** (ไทย) น้ำหนัก 400/500/600 | ข้อความทั้งหมด อังกฤษอยู่ก่อน คำอังกฤษในโหมดไทยจึงเป็นฟอนต์เดียวกับโหมดอังกฤษ |
| `--font-mono` | **Geist Mono** 400/500/600 | ตัวเลขในตารางและรายการ: เลขห้อง ยอดเงิน วันที่ หน่วยมิเตอร์ เบอร์โทร (class `.num`, `.room-plate`) ส่วนตัวเลขใหญ่ (`.figure-value`, `.amount`, `.kpi-value`, `.pay-amount-value`) ใช้ Geist ปกติ + tabular digits |

- หัวข้อหน้า 600, หัวข้อส่วน 600 ตัวเล็กลง (`--fs-base`) มีเส้นบางใต้หัวข้อ, label เล็ก 500 สี `--c-muted`
- อังกฤษ: หัวข้อใช้ `letter-spacing: -0.01em` ภาษาไทย (`:lang(th)`) ไม่บีบระยะ และ line-height 1.45 ให้สระบน-ล่างไม่ชนกัน
- `<html>` ทุก layout ต้องมี `th:lang="${#locale.language}"` เพื่อให้ `:lang(th)` ทำงาน
- `.num` ใส่ได้เฉพาะ element ที่เป็นตัวเลขล้วน (เช่น วันที่ ยอดเงิน) ประโยคที่มีคำไทยปนตัวเลขให้คงฟอนต์ข้อความ

| Token | ขนาด | ใช้กับ |
|---|---|---|
| `--fs-xs` | 12px | badge, หัวคอลัมน์, caption |
| `--fs-sm` | 14px | ตาราง, label, helper text, ปุ่ม |
| `--fs-base` | 16px | เนื้อหา, ช่องกรอก (กัน iOS ซูม) |
| `--fs-lg` | 18px | หัวการ์ด |
| `--fs-xl` | 22px | หัวหน้า |
| `--fs-2xl` | 28px | ตัวเลขสำคัญ |

ตัวเลขใช้ `font-variant-numeric: tabular-nums` ในฟอนต์ mono เสมอ ตัวเลขจึงตรงคอลัมน์

## Spacing, radius, shadow
- Spacing (4/8px): `--sp-1` 4 · `--sp-2` 8 · `--sp-3` 12 · `--sp-4` 16 · `--sp-5` 24 · `--sp-6` 32 · `--sp-7` 48 · `--sp-8` 64
- Radius: `--r-sm` 8px (ปุ่ม ช่องกรอก) · `--r-md` 12px (การ์ด modal) · `--r-pill`
- Shadow: ไม่ใช้กับการ์ด · `--shadow-overlay` เฉพาะ dropdown, modal, panel ที่ลอยบนแผนผัง 3D

## Component

| Component | Class / fragment | หมายเหตุ |
|---|---|---|
| Button | `.btn-primary` หลัก 1 ปุ่มต่อพื้นที่ · `.btn-secondary` ขอบเทา · `.btn-soft` การกระทำที่ใช้บ่อยในแถว · `.btn-ghost` · `.btn-danger` / `.btn-ghost-danger` · `.btn-icon` (ต้องมี `aria-label`) | สูง 40px, บน touch 44px |
| Input / Select | `.form-control`, `.form-select`, `.form-label` อยู่เหนือช่องเสมอ, `.form-text` ข้อความช่วย, `.is-invalid` + `.invalid-feedback` ใต้ช่อง, `.search-input` | 16px, สูง 44px |
| Card | `.card` ชั้นเดียว ห้ามซ้อน, `.card-header`, ไม่มีเงา | เฉพาะตารางหลักและฟอร์ม (ดู "ใช้การ์ดเมื่อไหร่") padding 24px (มือถือ 16px) |
| Section (ไม่มีกรอบ) | `section.plain-section` > `.section-head` (h2 + `.card-link`) + `ul.list-plain` / `.notice-list` / `.news-list` | หัวข้อ + เส้นบาง + รายการคั่นด้วย hairline |
| Today line | `p.today-line` บนสุดของ /admin: "วันนี้มี 3 บิลค้างชำระ · 2 งานซ่อมรอ · 0 สลิปรอตรวจ" แต่ละส่วนเป็นลิงก์ | ใช้ค่าจาก controller เท่านั้น ไม่มีงานค้างแสดง "วันนี้ไม่มีงานค้าง" |
| Figure row | `.figure-row` > `a.figure` > `.figure-label` + `.figure-value` (mono, `.is-alert` = สี danger) + `.figure-meta` | แถวเดียว ไม่มีกรอบ ไม่มีไอคอน คั่นด้วยเส้นตั้งบาง มือถือเป็น 2x2 |
| Room plate | `.room-plate` (`.room-plate-light` บนพื้นสีหลัก) | เลขห้องแบบป้ายหน้าห้อง: mono ตัวหนา กรอบ 1px มุม 4px |
| Plain list | `ul.list-plain` > `li` (`.item-title`, `.item-meta`, `.item-end`) | ใช้ระยะห่างแทนเส้นแบ่ง สูงอย่างน้อย 48px |
| Card link | `.card-link` ในหัวการ์ด ("ดูทั้งหมด") | ชิดขวา 44px บน touch |
| Quiet link | `.link-quiet` ลิงก์ในข้อมูล (เบอร์โทร `tel:`, อีเมล) | สีเหมือนข้อความ ขีดเส้นใต้ตอน hover/focus |
| Table | `.table` ใน `.table-wrap`; เพิ่ม `.table-cards` ให้แถวกลายเป็นการ์ดบนมือถือ, `td.td-title` หัวการ์ด, `td.td-actions` ปุ่ม; ถ้า cell มีหลายชิ้นในแถวเดียว (ค่า + badge) ให้ห่อด้วย `<span class="cell">`; ถ้าเป็นค่าหลัก + บรรทัดรอง ใช้ `<span class="cell-stack">` | label มาจากหัวคอลัมน์อัตโนมัติ (ui.js) |
| List toolbar | `.list-toolbar` + `input[data-filter-target]` + `select[data-filter-key]` + `[data-filter-count]` + `[data-filter-empty]`; แถวต้องมี `data-row`, ค่า `data-*` ใส่ได้หลาย token คั่นด้วยช่องว่าง (เช่น `data-status="unpaid overdue"`) | กรองฝั่งหน้าเว็บ ไม่แตะ API |
| Status | `~{fragments/ui :: status(tone, text)}` tone = success / info / warning / soon / danger / neutral (หรือชื่อใน enum `primary` / `secondary`) | จุด 8px + ข้อความ มีพื้นสีเฉพาะ danger; ถ้ามีเงื่อนไขให้ห่อด้วย `<th:block th:if>` เพราะ `th:replace` ทำงานก่อน `th:if` |
| Tag | `span.tag` | ป้ายที่ไม่ใช่สถานะ (หมวด, จำลอง, คุณ, ปักหมุด) ตัวเล็กสีจาง ไม่มีพื้น |
| Modal ยืนยัน | ใส่ `data-confirm="ลบห้อง A101 ใช่ไหม?"` ที่ลิงก์/ปุ่ม (+ `data-confirm-title`, `data-confirm-ok`, `data-confirm-tone="primary"` ถ้าไม่ใช่การลบ) | fragment `ui :: confirmModal` อยู่ใน layout แล้ว |
| Empty state | `~{fragments/ui :: emptyState(icon, title, text, href, label)}` | แสดงภาพลายเส้นหอขนาดเล็กเสมอ (`icon` ไม่ถูกใช้แล้ว) ส่ง `null` ถ้าไม่มีปุ่ม |
| ภาพลายเส้นหอ | `~{fragments/ui :: sjdDrawing(css, label)}` | inline SVG เส้นสี primary ไม่มี fill ใช้ในหน้า login/สมัคร และ empty state |
| เมนู | `.side-nav` (แอดมิน), `.bottom-nav` (ผู้เช่า) | รายการที่เลือก: แถบ 3px สี primary (ซ้ายใน sidebar, บนใน bottom nav) + ตัวหนา ไม่มีพื้นสี; ไอคอน 18px สีจาง |
| Login / สมัคร | `.auth-split` > `ui :: authArt` + `.auth-main` > `.auth-form` | จอ >= 992px แบ่งสองฝั่ง มือถือแสดงฟอร์มอย่างเดียวพร้อมภาพเล็ก ไม่มีการ์ดลอยกลางจอ |
| Flash | `~{fragments/ui :: flash}` | อยู่ใน layout แล้ว |
| Loading | อัตโนมัติกับทุกฟอร์ม POST (spinner + disable) | ปิดด้วย `data-no-loading` |
| Page header | `.page-header` > (`.page-title` + `.page-sub`) + ปุ่ม | ชิดซ้าย ไม่มีไอคอนประดับ ปุ่มหลักอยู่ขวา |
| Tabs | `.nav-pills` หรือ `.segmented` | |

## Accessibility checklist (ทุกหน้า)
- ไอคอน Lucide เป็น `aria-hidden` อัตโนมัติ ปุ่มที่มีแต่ไอคอนต้องมี `aria-label`
- focus ring มองเห็นได้เสมอ (`:focus-visible`)
- ทุกช่องกรอกมี `<label for>` ห้ามใช้ placeholder แทน label
- error แสดงใต้ช่องที่ผิด
- รองรับ `prefers-reduced-motion`

## ป้ายสถานะ
- ใช้ `ui :: status(tone, text)` ทุกที่: จุดสี 8px + ข้อความสีปกติ เช่น `● ชำระแล้ว`
- พื้นสีใช้เฉพาะสถานะเร่งด่วน (danger): ค้างชำระ, เลยกำหนด, สลิปน่าสงสัย, สัญญาหมดแล้ว
- ข้อความที่ไม่ใช่สถานะใช้ `.tag` ส่วนตัวเลขเปลี่ยนแปลงเช่น "+71%" ใช้ `.delta-up`

## ใช้การ์ดเมื่อไหร่
- ใช้: ตารางหลักของหน้า (บิล ห้อง ผู้เช่า ฯลฯ), ฟอร์ม, กล่องจ่ายเงินของบิลที่ค้าง, empty state ของรายการหลัก
- ไม่ใช้: ส่วนรอง (ฟีดแบ็ก, ค่าไฟผิดปกติ, ห้องซักผ้า, ประกาศ, การแจ้งเตือน, เพื่อนร่วมห้อง, ประวัติ, พัสดุ) ให้ใช้ `.plain-section`
- ตัวเลขสรุปใช้ `.figure-row` ไม่ใช่การ์ด

## ห้ามทำ
- การ์ดตัวเลขที่มีไอคอนมุมขวาบน (KPI card แบบเดิม `.kpi` ยังอยู่ใน CSS แต่ห้ามใช้ในหน้าใหม่)
- แคปซูลพื้นสีอ่อนกับทุกสถานะ (`badge soft-*`)
- เงาบนการ์ด หรือการ์ดซ้อนการ์ด
- การ์ดลอยกลางจอ (หน้า login แบบเดิม)
- ไอคอนประดับหน้าหัวข้อ, gradient, emoji, glassmorphism
- เปลี่ยนค่าสีใน tokens (`--c-*`, `--s-*`) โดยไม่ผ่าน stakeholder
