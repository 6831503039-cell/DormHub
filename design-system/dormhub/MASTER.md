# DormHub Design System (Master)

ทิศทาง: **minimal ใช้งานง่าย สะอาด มืออาชีพ** ใช้บนมือถือเป็นหลัก (เจ้าของหอและผู้เช่า)
ทุกค่าเก็บเป็น token ที่เดียวใน `src/main/resources/static/css/app.css` (`:root`) ห้ามเขียนค่าสี/ขนาดตรงๆ ใน template

## หลักการ
1. ข้อมูลสำคัญก่อน: ห้องว่าง, ยอดค้างชำระ, งานซ่อมที่รอ ต้องเห็นได้โดยไม่ต้องเลื่อน
2. Neutral เป็นหลัก สีหลักใช้กับการกระทำ (ปุ่มหลัก ลิงก์ เมนูที่เลือก) สีสถานะใช้กับสถานะเท่านั้น
3. ไม่มี gradient ไม่มีการ์ดซ้อนการ์ด ใช้ whitespace แทนเส้นแบ่ง เงาใช้เฉพาะของที่ลอย (dropdown, modal)
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

class badge เดิม (`soft-success`, `soft-warning` ฯลฯ) ใช้ต่อได้ และมีชื่อใหม่ `status-success`, `status-soon` ฯลฯ

## ตัวอักษร
ฟอนต์เดียว: **Anuphan** ทั้งภาษาไทย อังกฤษ และตัวเลข (ออกแบบมาคู่กัน ไม่ผสมฟอนต์อื่น) โหลดใน `fragments/fonts.html`

- น้ำหนัก 3 ระดับเท่านั้น: `--fw-regular` 400 · `--fw-medium` 500 · `--fw-semibold` 600 (`b`, `strong` = 600; ไม่มี 700 และไม่ใช้ตัวเอียง เพราะภาษาไทยไม่มีตัวเอียง)
- line-height `--lh` 1.65 (เนื้อหา) · `--lh-tight` 1.3 (หัวข้อ, ตัวเลขใหญ่)
- ไม่ใช้ตัวพิมพ์ใหญ่ทั้งคำ และไม่ปรับ letter-spacing
- ขนาดตัวอักษรทุกที่ (รวมแผนผัง 3D) ใช้ token ด้านล่างเท่านั้น

| Token | ขนาด | ใช้กับ |
|---|---|---|
| `--fs-xs` | 12px | badge, หัวคอลัมน์, caption |
| `--fs-sm` | 14px | ตาราง, label, helper text, ปุ่ม |
| `--fs-base` | 16px | เนื้อหา, ช่องกรอก (กัน iOS ซูม) |
| `--fs-lg` | 18px | หัวการ์ด |
| `--fs-xl` | 22px | หัวหน้า |
| `--fs-2xl` | 28px | ตัวเลขสำคัญ |

ตัวเลขของ Anuphan กว้างเท่ากันทุกตัว ยอดเงินในคอลัมน์จึงตรงกันโดยไม่ต้องใช้ฟอนต์ monospace; class `.num` ใส่ `tabular-nums` ไว้กันกรณีฟอนต์สำรอง

## Spacing, radius, shadow
- Spacing (4/8px): `--sp-1` 4 · `--sp-2` 8 · `--sp-3` 12 · `--sp-4` 16 · `--sp-5` 24 · `--sp-6` 32 · `--sp-7` 48 · `--sp-8` 64
- Radius: `--r-sm` 8px (ปุ่ม ช่องกรอก) · `--r-md` 12px (การ์ด modal) · `--r-pill`
- Shadow: `--shadow-sm` (การ์ด) · `--shadow-overlay` (dropdown, modal, panel ที่ลอย)

## Component

| Component | Class / fragment | หมายเหตุ |
|---|---|---|
| Button | `.btn-primary` หลัก 1 ปุ่มต่อพื้นที่ · `.btn-secondary` ขอบเทา · `.btn-soft` การกระทำที่ใช้บ่อยในแถว · `.btn-ghost` · `.btn-danger` / `.btn-ghost-danger` · `.btn-icon` (ต้องมี `aria-label`) | สูง 40px, บน touch 44px |
| Input / Select | `.form-control`, `.form-select`, `.form-label` อยู่เหนือช่องเสมอ, `.form-text` ข้อความช่วย, `.is-invalid` + `.invalid-feedback` ใต้ช่อง, `.search-input` | 16px, สูง 44px |
| Card | `.card` ชั้นเดียว ห้ามซ้อน, `.card-header` | padding 24px (มือถือ 16px) |
| Key number | `.stat` + `.stat-icon` (`green`/`amber`/`red`) + `.stat-value` + `.stat-label` | |
| KPI (Dashboard) | `a.card.kpi.tone-{success,warning,danger,info}` > `.kpi-head` (ชื่อ + ไอคอน) + `.kpi-value` + `.kpi-meta`; เพิ่ม `.is-alert` เมื่อต้องจัดการ ตัวเลขจะเป็นสีสถานะ | ไอคอนเป็นสีสถานะเสมอ |
| Plain list | `ul.list-plain` > `li` (`.item-title`, `.item-meta`, `.item-end`) | ใช้ระยะห่างแทนเส้นแบ่ง สูงอย่างน้อย 48px |
| Card link | `.card-link` ในหัวการ์ด ("ดูทั้งหมด") | ชิดขวา 44px บน touch |
| Quiet link | `.link-quiet` ลิงก์ในข้อมูล (เบอร์โทร `tel:`, อีเมล) | สีเหมือนข้อความ ขีดเส้นใต้ตอน hover/focus |
| Table | `.table` ใน `.table-wrap`; เพิ่ม `.table-cards` ให้แถวกลายเป็นการ์ดบนมือถือ, `td.td-title` หัวการ์ด, `td.td-actions` ปุ่ม; ถ้า cell มีหลายชิ้นในแถวเดียว (ค่า + badge) ให้ห่อด้วย `<span class="cell">`; ถ้าเป็นค่าหลัก + บรรทัดรอง ใช้ `<span class="cell-stack">` | label มาจากหัวคอลัมน์อัตโนมัติ (ui.js) |
| List toolbar | `.list-toolbar` + `input[data-filter-target]` + `select[data-filter-key]` + `[data-filter-count]` + `[data-filter-empty]`; แถวต้องมี `data-row`, ค่า `data-*` ใส่ได้หลาย token คั่นด้วยช่องว่าง (เช่น `data-status="unpaid overdue"`) | กรองฝั่งหน้าเว็บ ไม่แตะ API |
| Badge | `.badge.status-*` | สถานะต้องมีข้อความเสมอ ไม่ใช้สีอย่างเดียว |
| Modal ยืนยัน | ใส่ `data-confirm="ลบห้อง A101 ใช่ไหม?"` ที่ลิงก์/ปุ่ม (+ `data-confirm-title`, `data-confirm-ok`, `data-confirm-tone="primary"` ถ้าไม่ใช่การลบ) | fragment `ui :: confirmModal` อยู่ใน layout แล้ว |
| Empty state | `~{fragments/ui :: emptyState(icon, title, text, href, label)}` | ส่ง `null` ถ้าไม่มีปุ่ม |
| Flash | `~{fragments/ui :: flash}` | อยู่ใน layout แล้ว |
| Loading | อัตโนมัติกับทุกฟอร์ม POST (spinner + disable) | ปิดด้วย `data-no-loading` |
| Page header | `.page-header` > (`.page-title` + `.page-sub`) + ปุ่ม | |
| Tabs | `.nav-pills` หรือ `.segmented` | |

## Accessibility checklist (ทุกหน้า)
- ไอคอน Lucide เป็น `aria-hidden` อัตโนมัติ ปุ่มที่มีแต่ไอคอนต้องมี `aria-label`
- focus ring มองเห็นได้เสมอ (`:focus-visible`)
- ทุกช่องกรอกมี `<label for>` ห้ามใช้ placeholder แทน label
- error แสดงใต้ช่องที่ผิด
- รองรับ `prefers-reduced-motion`
