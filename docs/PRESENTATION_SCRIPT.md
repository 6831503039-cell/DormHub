# DormHub: 15-Minute Presentation Script

Group 15, School of Applied Digital Technology, Mae Fah Luang University.
Live site: https://dormhub-621353901508.us-central1.run.app

Speaking pace is about 130 words per minute. Text in *italics* is an action, not something to read aloud. Suggested speakers are Speaker 1 to Speaker 5, so each of the five members talks once.

| Time | Part | Speaker |
|---|---|---|
| 0:00 to 1:00 | Opening | 1 |
| 1:00 to 3:00 | Problem and vision | 1 |
| 3:00 to 5:00 | Requirements (SRS, use case diagram) | 2 |
| 5:00 to 6:30 | Design and technology | 3 |
| 6:30 to 11:30 | Live demo | 4 and 5 |
| 11:30 to 13:00 | Technical highlights | 3 |
| 13:00 to 14:00 | Deployment and testing | 2 |
| 14:00 to 15:00 | Limits, next steps, close | 1 |

---

## 0:00 to 1:00 Opening (Speaker 1)

Good morning, everyone. We are Group 15, and today we present **DormHub**, a web system that helps a dormitory owner and the people living in the dorm.

Running a dorm today often means paper, chat messages and a spreadsheet. The owner chases rent, tenants ask the same questions again and again, and nobody sees the whole picture.

In the next fifteen minutes we will show the problem, how we specified the system, how it is built, and then a live demo on the real, deployed website.

## 1:00 to 3:00 Problem and vision (Speaker 1)

Let us start with the problem. A dorm owner has to do five things all the time: keep room and tenant records up to date, send a bill every month, check that the money really arrived, answer repair requests, and tell everyone the news. Tenants have a matching list of needs: how much do I pay, how do I pay, who do I ask, and when does my parcel arrive.

Our vision is simple. **One system for both sides.** The owner gets one place to manage everything. The tenant gets one place, on a phone, to pay, ask and request.

DormHub is designed for any dormitory. For this course project we use one dorm as the mock-up, **SJD Mansion**, with 220 rooms and twelve months of sample bills, so that the demo feels real.

We also added AI where it saves real work. First, the AI reads a payment slip and decides if it is genuine. Second, a chatbot answers tenant questions using the dorm's own live data.

## 3:00 to 5:00 Requirements (Speaker 2)

Before writing code we wrote a Software Requirements Specification, an SRS, from the real code.

*Show the use case diagram (Figure 2).*

This is our use case diagram. It has **two people and one external system**. The **Owner** manages the dorm. The **User** is one actor that covers two kinds of person: a **Tenant**, who is logged in, and a **Visitor**, who just wants to look at rooms. The external system is **Gemini AI**.

There are **13 use cases**. The owner has seven: log in, change language and theme, manage rooms and tenants, manage billing, handle requests, post announcements and parcels, and view the dashboard and analytics. The user has the rest: pay bills, ask the AI assistant, browse rooms and book, use dorm services, read news and parcels, and register, rent a room and profile.

*Show the FR table.*

Each use case is a group of functional requirements, **FR-01 to FR-13**, and each group has detailed sub-requirements, **29 in total**. Every one has a description, input, output and a step-by-step workflow, so it can be tested.

We also wrote **18 non-functional requirements**, for example: passwords stored as BCrypt hashes, a tenant can only see their own bills, the system keeps working when the AI is down, and every page works on a phone.

## 5:00 to 6:30 Design and technology (Speaker 3)

Now the design. DormHub is one Spring Boot application with three areas.

- **/admin** for the owner.
- **/app** for tenants.
- **/explore** for anyone, no login needed.

Login is on one page. After login, the system sends the owner to /admin and a tenant to /app, and Spring Security blocks everything else.

The stack is **Java 17 and Spring Boot**, **Thymeleaf** for the pages, **Spring Data JPA** for the database, and **Google Gemini** for the AI. The database is **MySQL on Google Cloud SQL** in production. When no database is configured, the app falls back to an in-memory H2 database and creates demo data, so anyone can run it with one command.

The interface is in **Thai and English**, with **light and dark** themes.

## 6:30 to 11:30 Live demo (Speakers 4 and 5, 5 minutes)

*Open the deployed site. Keep a second tab logged in as the owner.*

**Step 1: Visitor, 45 seconds (Speaker 4).** Open `/explore`. "A visitor does not need an account. They see which rooms are free, the price, and the room details." Pick one room and show the booking request form.

**Step 2: Register and rent, 45 seconds (Speaker 4).** Register a new account. Show that it can only see Home, Rent a Room and Profile, because it has no room yet. Press rent on a room.

**Step 3: Owner confirms, 45 seconds (Speaker 5).** Switch to the owner tab, `sjdadmin@gmail.com`. Open the booking request and confirm it. "The system links the applicant to the room and starts a twelve month contract."

**Step 4: Tenant pays, 90 seconds (Speaker 5).** Log in as `somchai@example.com`. Open the home page and the bill. Show the share, the PromptPay QR, and upload a slip. "The AI reads the slip. It approves automatically only if it is a transfer slip, not edited, and the amount is within one baht. Otherwise it goes to the owner as pending."

**Step 5: Chatbot, 45 seconds (Speaker 4).** Ask "ห้องว่างตึก A มีอะไรบ้าง" or "How much is my bill this month?" "The answer uses the dorm's live data, so it is about *our* dorm, not a generic reply."

**Step 6: Owner overview, 60 seconds (Speaker 5).** In the owner tab show the dashboard, then the **room map** with search and the collapsible buildings, and the **3D view** of the dorm. Search a tenant by name or room number. Show the slip review and the electricity analytics with abnormal usage.

*If the internet or the AI is slow, say: "The system is built for this. Without the AI, the chatbot uses built-in answers and slips wait for the owner."*

## 11:30 to 13:00 Technical highlights (Speaker 3)

Three things we are proud of.

**One: business rules that are tested.** A bill is split between the tenants of a room and rounded to two decimals. A 3,000 baht bill shared by three people gives 1,000.00 each. The bill is paid when approved payments reach the total minus fifty satang. These rules, and the slip decision rules, are covered by automated unit tests.

**Two: real database search.** The owner's room and tenant search run in the database, through a Spring Data derived query and a JPQL query. It matches name, phone, email or room number, even for tenants without a room.

**Three: it fails gracefully and safely.** Passwords are BCrypt hashes. A tenant cannot open another room's bill, because that returns 404. Forms are protected against forged requests. The Gemini key, the admin password and the database password are environment variables, never in the code. Dates use the Asia/Bangkok time zone even though the cloud runs in UTC.

## 13:00 to 14:00 Deployment and testing (Speaker 2)

Deployment is automatic. Every push starts **Google Cloud Build**, which builds a Docker image, stores it in Artifact Registry, and deploys it to **Cloud Run** on port 8100. Cloud Run talks to **Cloud SQL** with settings stored in the environment.

One detail: login sessions live in memory, so we run one instance with session affinity. Otherwise a second instance would not know who is logged in.

For testing we run the unit tests with `./mvnw test`, and we checked each non-functional requirement against its acceptance criteria, for example the page width at 390 pixels and the slip size limit of 5 MB.

## 14:00 to 15:00 Limits, next steps, close (Speaker 1)

Honest limits. The laundry feature is a mock-up. There is no real payment gateway; payment is by PromptPay QR with a slip. The 3D room map is for the owner only.

Next steps: real-time laundry status, push notifications for due dates, a visitor-friendly photo gallery for each room, and support for more than one dorm, because DormHub is designed for any dormitory, not only SJD Mansion.

To close: DormHub gives the owner **one place to manage everything**, gives tenants **one place to pay and ask**, and uses AI only where it saves real work.

Thank you. We are happy to take questions.

---

## Backup answers for questions

- **Why Spring Boot and Thymeleaf?** It is what we learned in the course, it keeps the pages on the server, and Spring Security gives us role-based access.
- **What if Gemini is down or has no key?** The chatbot uses built-in keyword answers, and slips are saved as Pending for the owner. No error page appears.
- **How is the AI slip check safe?** It auto-approves only when all rules pass; everything else goes to the owner. The owner always has the final say.
- **Where is the data stored?** MySQL on Google Cloud SQL in production; in-memory H2 with demo data locally.
- **Who can see what?** `/admin` only for the ADMIN role, `/app` only for TENANT, `/explore` public.
- **Demo accounts:** owner `sjdadmin@gmail.com`; tenants `somchai@example.com`, `suda@example.com`, `manee@example.com`.
- **Why one actor "User" for Tenant and Visitor?** They share the same pages and goals; the difference is only whether they are logged in and have a room.

## Checklist before presenting

1. Open the live site once to wake it up, and log in as the owner and as a tenant in two tabs.
2. Keep a sample slip image ready to upload.
3. Have the SRS open to Figure 2 and section 3.1.
4. Practise the demo once with a timer. It must fit in five minutes.
