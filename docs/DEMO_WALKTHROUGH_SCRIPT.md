# DormHub: 5-Person Page-by-Page Walkthrough Script

Live site: https://dormhub-621353901508.us-central1.run.app (the demo logins are from the README).

The walkthrough follows one story, so every page has a reason to come next:
**a visitor finds a room, rents it, the owner confirms, the tenant pays and asks, the owner reviews the money and the requests, the tenant uses the dorm services.**

Person 1 to 5 are placeholders. Replace them with your names. Each person drives the screen during their own part and says the "Say" lines. The "Hand off" line passes the screen to the next person. Times add up to about 15 minutes.

| Order | Person | Pages | Time |
|---|---|---|---|
| 1 | Person 1 | Login, Register, Explore, Rent a Room | 3:00 |
| 2 | Person 4 | Booking requests, Tenants, Rooms, Room map, Dashboard (owner) | 3:30 |
| 3 | Person 2 | Home, Bills, Chat, Electricity usage (tenant) | 3:00 |
| 4 | Person 5 | Rent bills, Slip review, Repairs, Feedback, Electricity analytics (owner) | 3:00 |
| 5 | Person 3 | Laundry, Requests, Announcements, Parcels, Profile (tenant and owner) | 2:30 |

Before you start: open the site once to wake it up, and log in as the owner (`sjdadmin@gmail.com`) in one browser window and as a tenant (`somchai@example.com`) in a private window. Keep a sample slip image ready. Everyone should know the language switch (Thai and English) and the light and dark theme button, because it is on every page.

Text in *italics* is an action, not something to say aloud.

---

# PART 1: Person 1 (visitor and registration, 3:00)

## Page 1.1 Login: `/login` (0:30)

*Show the login page.*

**Say:** "Welcome to DormHub, our dormitory management system. Everyone enters through one login page. The system decides where you go: the owner goes to the admin area and a tenant goes to the tenant app. Passwords are stored as BCrypt hashes, and if you open a page you are not allowed to see, you are sent back here or get a 403. In the corner you can switch between Thai and English, and between light and dark."

*Click the language switch once, then back.*

**Code to mention if asked:** `security/SecurityConfig.java` (`/admin/**` for ADMIN, `/app/**` for TENANT, `/explore` public).

## Page 1.2 Register: `/register` (0:30)

*Click "Register".*

**Say:** "A new person can create an account. We ask for full name, email and a password of at least six characters, with confirmation. We check that the email is not already used and is not the reserved admin email. Every self-registered account gets the TENANT role, never admin."

*Register a demo account, for example `new@example.com`. Show that you are logged in.*

**Say:** "This account has no room yet. Look at the menu: it only offers Home, Rent a Room and Profile. That is on purpose, so a stranger cannot see any dorm data."

## Page 1.3 Explore (public room page): `/explore` (1:00)

*Open `/explore` in a new tab (or log out first). Do not log in.*

**Say:** "This page is public. A visitor does not need an account. They see which rooms are available, the price, the room type and how many people fit. They can look by building. When they like a room, they can send a booking request with their name, phone and move-in date."

*Open one available room and show the booking form. Do not submit if you want to keep the demo clean, or submit once.*

**Say:** "The 3D map is only for the owner, which keeps this page fast and simple on a phone."

**Code to mention:** `explore/` controllers, `BookingRequest` entity.

## Page 1.4 Rent a Room: `/app/rent` (1:00)

*Back in the new account, open "Rent a Room".*

**Say:** "A registered account with no room can ask to rent from inside the app. We pick a free room and send a request. The page confirms that the request was sent, and from then on we wait for the owner."

*Send the request for a room.*

**Say:** "In the database this is a `BookingRequest` row linked to the room and to our account, with status Pending. Now the owner has something to do."

**Hand off:** "Person 4, please switch to the owner view and confirm this request."

---

# PART 2: Person 4 (owner: rooms, tenants, dashboard, 3:30)

*Switch to the owner window: `sjdadmin@gmail.com`.*

## Page 2.1 Booking requests: `/admin/bookings` (0:40)

**Say:** "This is the owner area. The menu is grouped into Rooms, Finance and Services. Here is the booking request we just sent. The owner can see who asked, which room and the move-in date."

*Confirm the request.*

**Say:** "When the owner confirms, the system links the applicant to the room and starts a 12-month contract. The room becomes occupied, and the applicant gets a room in the app. The owner can also reject or cancel a request."

## Page 2.2 Tenants: `/admin/tenants` (0:50)

*Open Tenants.*

**Say:** "Here are all tenants with their room and contract end date. The search box searches in the database: name, phone, email or room number, even for tenants without a room."

*Type "somchai" or a room number in the search box.*

*Click "Add tenant".*

**Say:** "Adding or editing a tenant is a form. When we pick a room, the system checks the capacity. If the room is full we get a message. When the tenant moves in or out, the room's status updates by itself: occupied or available."

**Code to mention:** `admin/TenantController.java` lines 91 to 111 (save with capacity check and room status update), `TenantRepository.search` (JPQL).

## Page 2.3 Rooms: `/admin/rooms` (0:50)

*Open Rooms.*

**Say:** "Rooms are grouped by building and floor. Each building row shows free rooms out of total rooms, and it collapses to keep the page short. The search works on room number and room type."

*Search "A1". Open a room, then click add. Try saving a duplicate room number if you want to show the error.*

**Say:** "Room numbers are unique in the database. If we try the same number again, we get a clear message instead of an error page. A room that still has data cannot be deleted."

**Code to mention:** `admin/RoomController.java` lines 49 to 54 (search), 121 to 131 (save), 133 to 142 (delete).

## Page 2.4 Room map: `/admin/layout` (0:40)

*Open Room map.*

**Say:** "This is the room map. Each room is coloured by status: available, occupied or under maintenance. Click a room to see its details. There is also a 3D view of the building, which only the owner can see."

*Open the 3D view, turn it a little, click a room.*

## Page 2.5 Dashboard: `/admin` (0:30)

*Open Overview.*

**Say:** "The dashboard is the owner's first screen: free rooms, income, unpaid bills, and work waiting, such as slips to review, repairs and booking requests. Everything the owner needs to start the day is on one page."

**Hand off:** "Person 2, let's see what the tenant side looks like."

---

# PART 3: Person 2 (tenant: home, bills, chat, usage, 3:00)

*Switch to the tenant window: `somchai@example.com`, password `tenant123`.*

## Page 3.1 Home: `/app` (0:40)

**Say:** "This is the tenant's home page, designed for a phone: a bottom menu with Home, Bills, Ask AI, Laundry and Requests. At the top we see the room, the current bill and what is due. Below are the latest notifications: announcements, a parcel waiting, or a reminder before the due date."

*Narrow the browser window or open dev tools phone view to show the bottom menu.*

**Say:** "A tenant only sees data of their own room. If they try to open another room's bill, the server answers 404."

## Page 3.2 Bills: `/app/bills` (1:00)

*Open Bills.*

**Say:** "Here is the bill for the month: rent, water and electricity from the meter readings. If the room has several tenants, the bill is split and each person sees their own share, rounded to two decimals. For example, 3,000 baht for three people is 1,000.00 each."

*Open the pay screen and show the PromptPay QR.*

**Say:** "To pay, the tenant scans the PromptPay QR with a banking app. Then they upload the slip here."

*Upload the sample slip.*

**Say:** "Our AI reads the slip. It auto-approves only when it is a real transfer slip, not edited, and the amount is within one baht of what was entered. Anything doubtful becomes Pending or Suspicious and waits for the owner. The tenant sees the status right away."

## Page 3.3 Chat (virtual admin): `/app/chat` (0:50)

*Open Ask AI.*

**Say:** "This is our virtual admin. A tenant can ask about the bill, free rooms, parcels or announcements. The answer uses the dorm's live data, so it is about our dorm, not a general reply."

*Type: "How much is my bill this month?" Then: "Which rooms are free in building A?"*

**Say:** "If the AI key is missing or the AI is slow, the chatbot falls back to built-in answers. The page never shows an error."

## Page 3.4 Electricity usage: `/app/usage` (0:30)

*Open usage.*

**Say:** "This shows the room's electricity use over the past months. The tenant can see when the use went up, and the owner uses the same data for abnormal-use alerts."

**Hand off:** "Person 5, the tenant has uploaded a slip. Let's see what the owner sees."

---

# PART 4: Person 5 (owner: money, repairs, feedback, analytics, 3:00)

*Switch to the owner window.*

## Page 4.1 Rent bills: `/admin/bills` (0:40)

*Open Rent bills.*

**Say:** "Here the owner creates the monthly bills. We choose a room and a month, and enter the water and electricity meter units. If we leave the rent blank, the system uses the room's monthly rent. The due date is filled in automatically. The list shows which bills are paid and which are not."

*Click "Add bill" and show the form (do not save, or save for a demo room).*

**Code to mention:** `admin/BillController.java` lines 63 to 75.

## Page 4.2 Slip review: `/admin/payments` (0:50)

*Open Slip review.*

**Say:** "This is the slip we just uploaded. We see the image, the amount, and the AI's note on why it decided what it did. The owner approves or rejects. When approved payments reach the bill total, the bill becomes paid automatically."

*Approve the slip. Then open Rent bills and show the bill is now paid.*

**Say:** "The owner always has the final decision. The AI only saves time on clean slips."

## Page 4.3 Repairs: `/admin/repairs` (0:40)

*Open Repairs.*

**Say:** "Repairs reported by tenants arrive here. The owner changes the status, for example pending, in progress, done, and adds a note. What the tenant wrote stays as it was sent. "

## Page 4.4 Feedback: `/admin/feedback` (0:20)

*Open Feedback.*

**Say:** "Tenants can send feedback with a rating. Here the owner reads it and can filter it. Unread items are counted on the dashboard."

## Page 4.5 Electricity analytics: `/admin/analytics` (0:30)

*Open Electricity analytics.*

**Say:** "Last, analytics. We see income and electricity and water use over twelve months with seasons, and the system flags rooms with unusually high use, so the owner can check for a fault or a leak."

**Hand off:** "Person 3, finally the services around daily life."

---

# PART 5: Person 3 (services and news, 2:30)

## Page 5.1 Laundry: `/app/laundry` (0:30)

*Switch to the tenant window, open Laundry.*

**Say:** "Tenants can see which washers and dryers are free or busy and for how long. Be honest about this one: in this release the laundry page is a mock-up, so it shows the idea but is not connected to real machines."

## Page 5.2 Requests: `/app/requests` (0:40)

*Open Requests.*

**Say:** "A tenant can report a repair with a title and details, or send feedback with a rating. They can see the status of each repair afterwards."

*Submit a repair, for example "Air conditioner is not cold".*

*Switch to the owner window, open Repairs, show the new repair.*

**Say:** "It appears at once on the owner's side. That is the whole loop in two screens."

## Page 5.3 Announcements: `/app/announcements` and `/admin/announcements` (0:30)

*On the owner window, open Announcements and add one: "Water off Saturday 10:00 to 14:00".*

**Say:** "The owner writes an announcement with a title, text and an optional picture, and can pin it."

*Switch to the tenant window, open Announcements.*

**Say:** "Tenants read it in their app. The AI chatbot also knows the latest announcements."

## Page 5.4 Parcels: `/app/parcels` and `/admin/parcels` (0:30)

*On the owner window, open Parcels and add a parcel with a photo.*

**Say:** "When a parcel arrives, the owner records the room, the carrier and takes a photo."

*Switch to the tenant window, open Parcels and confirm pickup.*

**Say:** "The tenant sees it, comes to collect it and confirms the pickup. The owner can see who picked it up and when."

## Page 5.5 Profile: `/app/profile` (0:20)

*Open Profile.*

**Say:** "Last, the profile: name, email, a profile picture, and the language and theme. That completes the tour from a visitor looking for a room to a tenant living in the dorm."

**Close (Person 1 or 3):** "DormHub gives the owner one place to manage everything and gives tenants one place to pay, ask and request. Thank you. We are happy to take questions."

---

## Quick troubleshooting for the demo

| Problem | What to do |
|---|---|
| The site is slow on the first click | Open it a few minutes before presenting; Cloud Run may need to wake up. |
| The AI is slow or does not answer | Say: "Without the AI the chatbot uses built-in answers and slips wait for the owner." Continue. |
| The slip is not auto-approved | Say: "That is the safe path: doubtful slips go to the owner." Approve it as the owner. |
| You are logged out unexpectedly | Sessions are kept in memory on one server instance. Log in again and continue. |
| Demo data changed after earlier tries | Use another room or a new test email. |

## Page checklist (25 pages)

Visitor and account: Login, Register, Explore, Rent a Room.
Owner: Overview, Room map, Manage rooms, Tenants, Booking requests, Rent bills, Slip review, Repairs, Feedback, Electricity analytics, Announcements, Parcels.
Tenant: Home, Bills, Ask AI, Electricity use, Laundry, Requests, Announcements, Parcels, Profile.
