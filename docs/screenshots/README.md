# DormHub redesign: before / after (branch `design/distinct-look`)

Colours are unchanged (no token in `--c-*` / `--s-*` was touched). No Java, routes, form field names or
message keys were changed; every new text is in `messages.properties` and `messages_en.properties`.

## What changed
1. **Fonts**: Instrument Sans (English) + Anuphan (Thai) for text, JetBrains Mono for every number
   (room numbers, money, dates, units, phones). English headings are tightened by -0.01em; Thai (`:lang(th)`)
   keeps normal spacing and a taller line height.
2. **Status**: `ui :: status(tone, text)` = 8px dot + text. Only urgent states (unpaid, overdue,
   suspicious slip, expired contract) keep a tinted background.
3. **Menus**: the selected item has a 3px primary bar (left in the admin sidebar, on top in the tenant
   bottom menu) and bold text; 18px muted icons.
4. **Dashboard**: a "today" sentence with links, then one frameless row of figures (2 x 2 on phones).
   Secondary sections are a heading + hairline list; cards only for the main table and forms; no card shadows.
5. **Room plates**: `.room-plate` for room numbers (mono, 1px frame, 4px corners).
6. **Login / register**: split page with a line drawing of SJD (C left, A middle, B right) on wide screens;
   form only with a small drawing on phones. No floating card.
7. **Empty states**: the same drawing, small, instead of an icon.
8. Rules are in `design-system/dormhub/MASTER.md` (fonts, mono numbers, status dots, when to use a card,
   room plates, don'ts).

Checked: `mvn test` (51 tests pass, none needed changing), no horizontal overflow at 1440px or 390px on any
page below in Thai or English, no clashing Thai vowels, contrast pairs unchanged (only existing tokens are used).

## Screenshots (Thai; English versions in `after-en/`)

| Page | Before | After |
|---|---|---|
| /login 1440 | ![](before/login-1440.png) | ![](after/login-1440.png) |
| /login 390 | ![](before/login-390.png) | ![](after/login-390.png) |
| /admin 1440 | ![](before/admin-1440.png) | ![](after/admin-1440.png) |
| /admin 390 | ![](before/admin-390.png) | ![](after/admin-390.png) |
| /admin/bills 1440 | ![](before/admin-bills-1440.png) | ![](after/admin-bills-1440.png) |
| /admin/bills 390 | ![](before/admin-bills-390.png) | ![](after/admin-bills-390.png) |
| /admin/rooms 1440 | ![](before/admin-rooms-1440.png) | ![](after/admin-rooms-1440.png) |
| /admin/rooms 390 | ![](before/admin-rooms-390.png) | ![](after/admin-rooms-390.png) |
| /admin/layout 1440 | ![](before/admin-layout-1440.png) | ![](after/admin-layout-1440.png) |
| /admin/layout 390 | ![](before/admin-layout-390.png) | ![](after/admin-layout-390.png) |
| /app 1440 | ![](before/app-1440.png) | ![](after/app-1440.png) |
| /app 390 | ![](before/app-390.png) | ![](after/app-390.png) |
| /app/bills 1440 | ![](before/app-bills-1440.png) | ![](after/app-bills-1440.png) |
| /app/bills 390 | ![](before/app-bills-390.png) | ![](after/app-bills-390.png) |

English (after): [login 390](after-en/login-390.png) · [admin 390](after-en/admin-390.png) ·
[app 390](after-en/app-390.png) · [app/bills 390](after-en/app-bills-390.png) · [admin 1440](after-en/admin-1440.png)

Screenshots are full-page captures from headless Chrome (pages longer than 6000px are cut at 6000px).
