# DormHub Design System (Master)

ทิศทาง: **สมุดทะเบียนหอ** กระดาษ หมึก เส้นบรรทัด เหมือนสมุดบัญชีที่สำนักงานหอใช้จริง ใช้บนมือถือเป็นหลัก (ผู้เช่า) และจอคอม (เจ้าของหอ)
ทุกค่าเก็บเป็น token ที่เดียวใน `src/main/resources/static/css/app.css` (`:root` และ `:root[data-bs-theme="dark"]`) ห้ามเขียนค่าสี/ขนาดตรงๆ ใน template

## หลักการ
1. **เส้นแทนกล่อง** section คือเส้นหมึก 2px กับของที่อยู่ใต้เส้น แถวคั่นด้วยเส้นบาง ไม่มีการ์ดขอบมน ไม่มีเงาบนหน้า เงาใช้เฉพาะของที่ลอย (dropdown, modal, แผงบนแผนที่ 3D)
2. **แต่ละหน้ามีของดังที่สุดอย่างเดียว** หน้าผู้เช่าคือยอดที่ต้องจ่าย หน้าเจ้าของหอคือยอดค้างชำระ ตัวเลขอื่นเล็กกว่าอย่างชัดเจน ห้ามจัดสถิติเป็นช่องเท่ากันเรียงแถว
3. **สีมีความหมายเท่านั้น** พื้นเป็น neutral สีหลักใช้กับการกระทำ สีสถานะใช้เมื่อมีคนต้องทำอะไร ตัวเลขปกติเป็นสีหมึก
4. **ตัวหนังสือมาก่อนไอคอน** ไอคอนใช้เฉพาะแถบล่างมือถือ ปุ่มที่มีแต่ไอคอน และในช่องกรอก ห้ามวางไอคอนในกล่องสีหน้าหัวข้อ ห้ามใช้ไอคอน sparkles
5. ทุกปุ่ม/ลิงก์ที่กดได้บนมือถือ สูงอย่างน้อย 44px, contrast ข้อความอย่างน้อย 4.5:1 ทั้งสองธีม

## สิ่งที่ห้ามกลับไปใช้
การ์ดขอบมนซ้ำๆ · ไอคอนในกล่อง/วงกลมหน้าหัวข้อ · KPI 4 ช่องเท่ากัน · ปุ่มลัดไอคอนตาราง 3×2 · badge ทรง pill พื้นอ่อน · ดาว 5 ดวง (ใช้ `4/5`) · gradient · glass/blur · ขอบซ้ายสีบนกล่อง · คำทักทายแบบ "สวัสดี, ชื่อ" · ข้อความแนวการตลาด

## สี

มาจากโลโก้: เขียวเสจกดให้เข้มเป็น forest, ทรายใช้เป็นแสงหน้าต่าง, ครีมเป็นกระดาษ

| Token | สว่าง | มืด | ใช้กับ |
|---|---|---|---|
| `--c-bg` | `#f3f0e7` | `#141713` | กระดาษ พื้นหน้า |
| `--c-surface` | `#fbf9f3` | `#1d211c` | ช่องกรอก และของที่ลอย |
| `--c-surface-2` / `--c-hover` | `#ebe7db` / `#e9e5d8` | `#242923` / `#262b25` | พื้นจม, hover แถว |
| `--c-ink` | `#1d2823` | `#cfd2c4` | เส้น section, ขอบปุ่มรอง, เมนูที่เลือก |
| `--c-border` / `--c-border-strong` | `#d8d3c4` / `#928d7c` | `#343a33` / `#6f766d` | เส้นบางระหว่างแถว / ขอบช่องกรอก |
| `--c-text` / `--c-text-2` / `--c-muted` | `#1d2823` / `#3f4944` / `#5b635c` | `#ece9df` / `#c9ccc1` / `#a3a89e` | ข้อความ |
| `--c-primary` / `--c-on-primary` | `#2b5a44` / `#fbf9f3` | `#a8c6a9` / `#14201a` | ปุ่มหลักเท่านั้น (ในธีมมืดตัวหนังสือบนปุ่มเป็นสีเข้ม) |
| `--c-primary-text` | `#265039` | `#b4d2b5` | ลิงก์ |
| `--c-hero` / `--c-hero-text` / `--c-hero-muted` | `#203229` / `#f4efe2` / `#bcc7b8` | `#243a2e` / เหมือนกัน / `#c3cdbf` | ใบแจ้งยอดหน้าแรก, แผงหน้าล็อกอิน |
| `--c-window` / `--c-window-off` | `#e0c48f` / `#2d4438` | เหมือนกัน / `#304a3c` | หน้าต่างอาคาร |

### สีสถานะ

| สถานะ | Token | dot | fg | ใช้กับ |
|---|---|---|---|---|
| success | `--s-success*` | `#2e9a5a` | `#196a3b` | ว่าง, ชำระแล้ว, ซ่อมเสร็จ, อนุมัติ |
| info | `--s-info*` | `#5b7a9d` | `#40597a` | มีผู้เช่า, ฤดูฝน |
| warning | `--s-warning*` | `#d99a00` | `#805310` | รอซ่อม, ปิดซ่อม, สลิปรอตรวจ, ฤดูร้อน |
| soon | `--s-soon*` | `#e8833a` | `#9c410b` | ใกล้หมดสัญญา |
| danger | `--s-danger*` | `#d92d20` | `#b42318` | ค้างชำระ, เลยกำหนด, สลิปน่าสงสัย |
| neutral | `--s-neutral*` | `#a3a8a2` | `#4a524e` | ไม่อนุมัติ, อื่นๆ, ฤดูหนาว |

สีสถานะในแผนผัง 3D (`static/js/dorm3d.js` → `COLORS`, `CSS_COLORS`) ต้องตรงกับค่า dot ในตารางนี้

## ตัวอักษร
**Geist** (ละติน) + **Anuphan** (ไทย) + **Geist Mono** (ตัวเลข) โหลดที่ `fragments/fonts.html` น้ำหนัก 400 / 500 / 600 (Mono ถึง 500), line-height 1.65

Geist Mono ใช้กับสิ่งที่เป็นตัวเลขล้วนเท่านั้น: เลขห้อง จำนวนเงินในตาราง วันที่ (class `.num`) ถ้าข้อความมีคำไทยปน ให้ใช้ `--font` กับ `tabular-nums` เพราะช่องว่างของ Mono กว้างเกินไปสำหรับภาษาไทย

| Token | ขนาด | ใช้กับ |
|---|---|---|
| `--fs-xs` | 12px | สถานะ, หัวคอลัมน์, caption |
| `--fs-sm` | 14px | ตาราง, label, ปุ่ม |
| `--fs-base` | 16px | เนื้อหา, ช่องกรอก (กัน iOS ซูม) |
| `--fs-lg` | 19px | หัว section |
| `--fs-xl` | 30px | หัวหน้า (มือถือ 26px) |
| `--fs-2xl` | 36px | ตัวเลขรอง |
| `--fs-3xl` | 52px | ตัวเลขนำของหน้า |

## Spacing, radius, shadow
- Spacing: `--sp-1` 4 · `--sp-2` 8 · `--sp-3` 12 · `--sp-4` 16 · `--sp-5` 24 · `--sp-6` 32 · `--sp-7` 48 · `--sp-8` 64
- Radius: `--r-sm` 3px (ปุ่ม ช่องกรอก) · `--r-md` 5px (modal) · ของกลมมีแค่รูปโปรไฟล์
- เส้น: `--rule` หมึก 2px เปิด section · `--hair` เส้นบางระหว่างแถว
- Shadow: `--shadow-overlay` เฉพาะของที่ลอย

## Component

| Component | Class / fragment | หมายเหตุ |
|---|---|---|
| Section | `.card` + `.card-header` + `.card-body` | ไม่มีกล่อง เส้นหมึกด้านบนเท่านั้น |
| แถวเรียงกัน | `.rows` > ลูกโดยตรง | ลูกแรกเปิดด้วยเส้นหมึก ที่เหลือเส้นบาง |
| ระยะระหว่าง section | `.stack-sections` | 48px (มือถือ 32px) |
| ตัวเลขนำ (เจ้าของหอ) | `.ledger` > `a.ledger-cell` (`.is-lead`, `.is-danger`, `.is-warning`) > `.ledger-label` `.ledger-figure` `.ledger-note` | ช่องแรกกว้างกว่า บนมือถือเป็นแถว |
| ตัวเลขรอง | `.kpi` > `.kpi-head` `.kpi-value` `.kpi-meta` | |
| ใบแจ้งยอด (ผู้เช่า) | `.hero` > `.hero-room` `.hero-plate` `.hero-label` `.amount` | ขอบล่างรอยฉีก เต็มขอบจอ; `.is-clear` เมื่อไม่มียอดค้างจะไม่มีพื้น |
| สารบัญบริการ | `.directory.rows` > `a.dir-row` > ชื่อ + `.dir-value` (`.is-alert`) | ค่าทางขวาบอกสถานะจริง |
| แจ้งเตือน | `.notice.{info,warning,danger}` > `.notice-title` | แถบพื้นสีอ่อน ไม่มีไอคอน |
| บอร์ดประกาศ | `.news-item` > `.news-date` + เนื้อหา | วันที่อยู่ในขอบซ้าย |
| แผนผังห้อง | `.floor-row` > `a.room-tile.{AVAILABLE,OCCUPIED,MAINTENANCE,SOON}` | ห้องติดกันใช้ผนังร่วม ปิดซ่อมเป็นลายเส้นทแยง |
| ซักผ้า | `.machine` + `.machine-bar` | แถบบางบอกเวลาที่เหลือ |
| หน้าล็อกอิน | `.auth-split` > `ui :: authSide` + `.auth-main` > `.auth-form` | อาคารตอนกลางคืน + ฟอร์ม |
| Button | `.btn-primary` 1 ปุ่มต่อพื้นที่ · `.btn-secondary` ขอบหมึก · `.btn-soft` การกระทำในแถว · `.btn-ghost` · `.btn-danger` / `.btn-ghost-danger` · `.btn-icon` (ต้องมี `aria-label`) | สูง 40px, touch 44px |
| Input / Select | `.form-control`, `.form-select`, `.form-label` อยู่เหนือช่องเสมอ, `.form-text`, `.is-invalid` + `.invalid-feedback`, `.search-input` | 16px, สูง 44px |
| Plain list | `ul.list-plain` > `li` (`.item-title`, `.item-meta`, `.item-end`) | |
| คะแนน | `.score` (`.is-low`) | `4/5` |
| Card link | `.card-link` ในหัว section | |
| Quiet link | `.link-quiet` | |
| Table | `.table` ใน `.table-wrap`; `.table-cards` ให้แถวซ้อนบนมือถือ, `td.td-title`, `td.td-actions`, `<span class="cell">`, `<span class="cell-stack">` | label มาจากหัวคอลัมน์อัตโนมัติ (ui.js) |
| List toolbar | `.list-toolbar` + `input[data-filter-target]` + `select[data-filter-key]` + `[data-filter-count]` + `[data-filter-empty]`; แถวต้องมี `data-row` | กรองฝั่งหน้าเว็บ |
| Status | `.badge.status-*` (class เดิม `soft-*` ใช้ได้) | คำ + สี่เหลี่ยมเล็ก มีพื้นทึบเฉพาะ danger สถานะต้องมีข้อความเสมอ |
| Modal ยืนยัน | `data-confirm="..."` (+ `data-confirm-title`, `data-confirm-ok`, `data-confirm-tone="primary"`) | fragment `ui :: confirmModal` |
| Empty state | `~{fragments/ui :: emptyState(title, text, href, label)}` | ประโยคธรรมดาชิดซ้าย ส่ง `null` ถ้าไม่มีปุ่ม |
| Flash | `~{fragments/ui :: flash}` | |
| Loading | อัตโนมัติกับทุกฟอร์ม POST | ปิดด้วย `data-no-loading` |
| Page header | `.page-header` > `.page-title` + `.page-sub` + ปุ่ม | |
| Tabs | `.segmented` (คำบนเส้น), `.nav-pills` | |

## ข้อความใน UI
เขียนสั้น บอกข้อเท็จจริง ใช้คำที่คนในหอพูดกัน ไม่มีคำโปรย ไม่ขายของ ไม่ขีดยาว ข้อความไทยอยู่ `messages.properties` อังกฤษอยู่ `messages_en.properties` ใช้ key เดียวกัน

## Accessibility checklist (ทุกหน้า)
- ไอคอน Lucide เป็น `aria-hidden` อัตโนมัติ ปุ่มที่มีแต่ไอคอนต้องมี `aria-label`
- focus ring มองเห็นได้เสมอ (`:focus-visible`)
- ทุกช่องกรอกมี `<label for>` ห้ามใช้ placeholder แทน label
- error แสดงใต้ช่องที่ผิด
- รองรับ `prefers-reduced-motion`
