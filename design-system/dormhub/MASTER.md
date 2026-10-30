# DormHub Design System (Master)

ทิศทาง: **minimal, clean** ใช้ได้ทั้งมือถือ (ผู้เช่า) และเดสก์ท็อป (เจ้าของหอ)
ค่าทั้งหมดอยู่ใน `src/main/resources/static/css/tokens.css` ส่วน `app.css` อ้างอิงแค่ token ชั้น semantic และ component
ห้ามเขียนค่าสี ขนาด หรือฟอนต์ตรง ๆ ใน template

## หลักการ
1. ข้อมูลสำคัญอยู่บนสุด: ยอดค้างชำระ ห้องว่าง งานซ่อมที่รอ
2. สีเทาเป็นหลัก มีสีเน้นสีเดียวคือ sage จากโลโก้ ใช้กับลิงก์ เมนูที่เลือก focus ring และสถานะ selected เท่านั้น
3. ปุ่มหลักใช้สีเกือบดำ
4. ใช้เส้น 1px และ whitespace แบ่งส่วน ไม่ใช้กล่องหรือเงา เงามีเฉพาะของที่ลอย (dropdown, modal, แผงบนแผนที่)
5. มุมโค้ง 4–6px, ไม่มี gradient, glass หรือ emoji
6. สถานะใช้สีหม่น และมีคำกำกับเสมอ
7. animation มีแค่ hover/focus (150ms) และเปิด/ปิด (200ms) และรองรับ `prefers-reduced-motion`
8. ทุกจุดที่กดได้บนจอสัมผัสสูงอย่างน้อย 44px และข้อความผ่าน contrast 4.5:1

## โครงสร้าง token (3 ชั้น)

```
primitive   --p-*                ค่าดิบ เช่น --p-sage-700: #536349
   ↓
semantic    --c-* --s-* --fs-*   หน้าที่ เช่น --c-primary-text: var(--p-sage-700)
   ↓                             (ธีมสว่าง/มืดเปลี่ยนเฉพาะชั้นนี้)
component   --btn-* --input-*    ค่าของ component เช่น --btn-primary-bg: var(--c-ink)
```

## สี: primitive

| Token | ค่า | หมายเหตุ |
|---|---|---|
| `--p-white` | `#ffffff` | |
| `--p-grey-50` / `100` / `200` / `300` | `#f6f6f3` / `#f0f0ed` / `#e4e4e0` / `#cfcfca` | พื้นเงียบ, hover, เส้น |
| `--p-grey-400` | `#8f969c` | ขอบช่องกรอก (3:1) |
| `--p-slate-600` | `#5c6670` | เทาอมฟ้าจากตัวอักษรโลโก้ (`#788490`) ทำให้เข้มขึ้น |
| `--p-grey-700` / `800` / `900` | `#3f464c` / `#2b3034` / `#1a1d1f` | ข้อความ และปุ่มหลัก |
| `--p-sage-500` | `#8a9c80` | sage จากโลโก้ ใช้ตกแต่ง (แถบเมนู active) ห้ามใช้เป็นตัวหนังสือ |
| `--p-sage-600` / `700` | `#5e6e55` / `#536349` | focus ring / ข้อความและลิงก์สีเน้น |
| `--p-sage-50` / `100` / `300` | `#f2f4f0` / `#e7ece4` / `#a9b99f` | พื้น selected / hover / สีเน้นบนธีมมืด |
| `--p-dark-950` … `100` | `#121414` … `#e8eaea` | ชุดเทาของธีมมืด |
| สถานะ `--p-{green,blue,ochre,rust,red}-{50,300,400,600,950}` | ดูตารางสถานะ | 50 พื้นสว่าง, 300 ตัวหนังสือบนมืด, 400 จุด, 600 ตัวหนังสือบนสว่าง, 950 พื้นมืด |

ห้ามใช้สีทรายจากโลโก้ใน UI (ใช้ได้เฉพาะในไฟล์โลโก้)

## สี: semantic

| Token | สว่าง | มืด | ใช้กับ | Contrast |
|---|---|---|---|---|
| `--c-bg`, `--c-surface` | `#ffffff` | `#121414` / `#181b1c` | พื้นหน้า | |
| `--c-surface-2` | `#f6f6f3` | `#1e2122` | พื้นเงียบ, hover แถวตาราง | |
| `--c-hover` | `#f0f0ed` | `#24282a` | hover รายการ/ปุ่ม | |
| `--c-border` | `#e4e4e0` | `#2e3335` | เส้นแบ่ง | |
| `--c-border-strong` | `#8f969c` | `#6b7377` | ขอบช่องกรอก, ปุ่มรอง | 3:1 / 3.6:1 |
| `--c-text` | `#1a1d1f` | `#e8eaea` | ข้อความหลัก | 16.9:1 / 14.3:1 |
| `--c-text-2` | `#3f464c` | `#c3c8ca` | ข้อความรอง | 9.6:1 |
| `--c-muted` | `#5c6670` | `#9aa3aa` | คำอธิบาย, หัวตาราง, placeholder | 5.85:1 / 6.8:1 |
| `--c-ink` | `#1a1d1f` | `#e8eaea` | พื้นปุ่มหลัก | ตัวหนังสือบนปุ่ม 16.9:1 / 15.3:1 |
| `--c-primary-text` | `#536349` | `#a9b99f` | ลิงก์ เมนู active | 6.5:1 / 8.4:1 |
| `--c-primary-soft` | `#f2f4f0` | `#252b23` | พื้นเมนู active / selected | ตัวหนังสือสีเน้นบนพื้นนี้ 5.8:1 / 7:1 |
| `--c-primary-mark` | `#8a9c80` | `#8a9c80` | แถบ 2px ของเมนู active | |
| `--c-focus` | `#5e6e55` | `#a9b99f` | focus ring 2px | 5.5:1 |

## สีสถานะ (จุด + คำ เสมอ)

| สถานะ | Token | จุด | ตัวหนังสือ / พื้น (สว่าง) | ตัวหนังสือ / พื้น (มืด) | Contrast สว่าง / มืด | ใช้กับ |
|---|---|---|---|---|---|---|
| success | `--s-success*` | `#6e9a7e` | `#3d6b4f` / `#eef4f0` | `#8fbf9f` / `#1c2a21` | 5.5 / 7.2 | ชำระแล้ว, ว่าง, ซ่อมเสร็จ |
| info | `--s-info*` | `#7d8ea0` | `#4e5f70` / `#eff2f5` | `#a3b6ca` / `#1e252d` | 5.9 / 7.4 | มีผู้เช่า |
| warning | `--s-warning*` | `#b8913f` | `#7a5c24` / `#f7f2e7` | `#d4b06a` / `#2b2518` | 5.6 / 7.4 | รอซ่อม, ปิดซ่อม, สลิปรอตรวจ |
| soon | `--s-soon*` | `#c27b55` | `#94502c` / `#f8efea` | `#e0a47e` / `#2d211b` | 5.4 / 7.3 | ใกล้หมดสัญญา |
| danger | `--s-danger*` | `#c0695e` | `#a2463b` / `#f8edeb` | `#e39a8f` / `#2f1f1d` | 5.3 / 7.0 | ค้างชำระ, เลยกำหนด |
| neutral | `--s-neutral*` | `#a3a9ae` | `#5c6670` / `#f0f0ed` | `#c3c8ca` / `#24282a` | 5.1 / 8.8 | หมวดหมู่, ไม่อนุมัติ (ไม่มีจุด) |

แผนผัง 3D (`static/js/dorm3d.js`) ยังใช้สีสถานะชุดเดิมในโมเดล เพราะไฟล์นี้ห้ามแก้

## ตัวอักษร

| Token | ค่า | ใช้กับ |
|---|---|---|
| `--font` | `'Geist', 'Anuphan', system-ui, sans-serif` | ทุกข้อความ (ตัวไทยใช้ Anuphan) |
| `--font-mono` | `'Geist Mono', 'Anuphan', ui-monospace, monospace` + `tabular-nums` | เลขห้อง จำนวนเงิน มิเตอร์ (`.num`, ตัวเลขในตาราง, `input[type=number]`) |
| `--fs-xs` | 12px | หัวตาราง, ป้ายสถานะ |
| `--fs-sm` | 14px | ตาราง, label, ปุ่ม |
| `--fs-base` | 16px | เนื้อหา, ช่องกรอก |
| `--fs-lg` | 18px | หัว modal |
| `--fs-xl` | 24px | หัวหน้า |
| `--fs-2xl` | 32px | ตัวเลขที่สำคัญที่สุดของหน้า |
| `--lh` / `--lh-tight` | 1.65 / 1.3 | เนื้อหา / หัวข้อ (เผื่อสระบนล่างของไทย) |
| น้ำหนัก | 400 / 500 / 600 | เนื้อหา / label, ตัวเลข / หัวข้อ |

ฟอนต์โหลดครั้งเดียวที่ `templates/fragments/head.html`

## Spacing, radius, motion

| Token | ค่า |
|---|---|
| `--sp-1` … `--sp-8` | 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64 px |
| `--r-sm` | 4px (ปุ่ม ช่องกรอก ป้ายสถานะ) |
| `--r-md` | 6px (modal, แผงบนแผนที่) |
| `--shadow-overlay` | `0 4px 16px rgba(26,29,31,.08)` เฉพาะของที่ลอย |
| `--dur` / `--dur-open` | 150ms (hover, focus) / 200ms (เปิด/ปิด) |
| `--tap` | 44px |

## Component token

| Token | ค่า |
|---|---|
| `--btn-h` | 40px (44px บนจอสัมผัส) |
| `--btn-primary-bg` / `-hover` / `-fg` | `--c-ink` / `--c-ink-hover` / `--c-on-ink` |
| `--btn-secondary-bg` / `-border` | `--c-surface` / `--c-border-strong` |
| `--input-h` / `--input-border` / `--input-radius` | 44px / `--c-border-strong` / `--r-sm` |
| `--table-head-fg` / `--table-row-border` | `--c-muted` / `--c-border` |
| `--nav-active-bg` / `-fg` / `-mark` | `--c-primary-soft` / `--c-primary-text` / `--c-primary-mark` |
| `--badge-radius` | `--r-sm` |
| `--focus-ring` | วงขาว 2px + `--c-focus` 2px |
| `--logo-size` | 32px |

## โลโก้และ favicon
แก้ได้ที่จุดเดียวคือ `templates/fragments/head.html` (fragment `logo` และ `<link rel="icon">` ใน `assets`)

## Component

| Component | Class / fragment | หมายเหตุ |
|---|---|---|
| Section | `.card` + `.card-header` | ไม่มีกล่อง: หัวข้อ + เส้นใต้ 1px เนื้อหาชิดซ้าย |
| ตัวเลขสรุป | `.figures` (`.cols-2`, `.cols-3`) > `.kpi` (`.kpi-head`, `.kpi-value`, `.kpi-meta`) | แถบเดียวคั่นด้วยเส้นตั้ง ตัวแรกใหญ่สุด `.is-alert.tone-danger` ทำให้ตัวเลขเป็นสีสถานะ |
| Button | `.btn-primary` (ดำ, ใช้ 1 ปุ่มต่อพื้นที่) · `.btn-secondary` · `.btn-soft` · `.btn-ghost` · `.btn-danger` · `.btn-icon` (ต้องมี `aria-label`) | |
| Input | `.form-control`, `.form-select`, label อยู่เหนือช่องเสมอ | |
| Table | `.table` ใน `.table-wrap`, `.table-cards` สำหรับมือถือ | หัวตารางเล็ก ตัวเลขชิดขวาแบบ mono ไม่มีสีสลับแถว |
| Status | `.badge.status-*` | จุดสี + คำ |
| List | `ul.list-plain` | แถวคั่นเส้นบาง |
| Notice | `.notice.{info,warning,danger}` | แถบสีซ้าย 2px |
| Tabs | `.segmented` | เส้นใต้ |
| Empty state | `~{fragments/ui :: emptyState(...)}` | ข้อความชิดซ้าย ไม่มีไอคอนวงกลม |

## Accessibility checklist (ทุกหน้า)
- ไอคอน Lucide ชุดเดียว เส้น 1.5px ปุ่มที่มีแต่ไอคอนต้องมี `aria-label`
- focus ring มองเห็นได้เสมอ (`:focus-visible`)
- ทุกช่องกรอกมี `<label for>` ห้ามใช้ placeholder แทน label
- error แสดงใต้ช่องที่ผิด
- จุดที่กดได้ 44px บนจอสัมผัส
