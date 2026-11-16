# DormHub: Likely Questions and Answers

Answers are based on the code on `main` and the SRS v1.4. Where an answer admits a weakness, say it openly and give the next step: that scores better than defending it.

## A. Essential questions (expect these)

**1. What problem does DormHub solve?**
Dorm owners track rooms, rent and requests on paper or in chat, and tenants keep asking the same questions. DormHub puts both sides in one web app: the owner manages everything, the tenant pays, asks and requests from a phone.

**2. Who are the users?**
Three actors in the use case diagram: the Owner, the User (a Tenant who is logged in, or a Visitor who is not), and Gemini AI as an external system.

**3. What are the main features?**
Owner: rooms and tenants, monthly bills with meter readings, slip review, repairs, booking requests, parcels, announcements, dashboard and analytics. Tenant: bills and PromptPay QR, slip upload, repair requests, parcels, laundry status, usage history, AI chatbot, rent a room. Visitor: browse free rooms and request a booking.

**3b. Why one actor for Tenant and Visitor?**
They use the same screens and goals. The only difference is whether they are logged in and have a room, so the diagram groups them to stay readable.

**4. What technologies did you use and why?**
Java 17, Spring Boot 2.7, Thymeleaf, Spring Data JPA, Spring Security, MySQL on Cloud SQL, Gemini API, Docker, Cloud Build, Cloud Run. They match what we learned in the course, and Spring Security gives role-based access with little code.

**5. How does login work and how do you separate owner and tenant?**
One login page. Passwords are BCrypt hashes. `/admin/**` is allowed only for role ADMIN, `/app/**` only for TENANT, and `/explore` is public. After login, `/` redirects each role to its own area.

**6. How does the AI slip check work?**
Gemini reads the slip image. The system auto-approves only if it is a transfer slip, not tampered, and the amount is within 1 baht of the entered amount. Everything else becomes Pending or Suspicious and goes to the owner, who has the final decision.

**7. How does the chatbot work?**
It sends the question and the dorm's live data (bills, free rooms, announcements, repairs) to Gemini, so answers are about this dorm. Without an API key, or if Gemini is late, it uses built-in keyword answers.

**8. How is the bill calculated?**
Rent plus water and electricity from meter units, split between the tenants of the room and rounded to 2 decimals. A 3,000 baht bill for 3 tenants gives 1,000.00 each. The bill is marked paid when approved payments reach the total minus 0.50 baht.

**9. Where is the data stored?**
MySQL on Google Cloud SQL in production. Without `DB_URL` the app uses in-memory H2 with generated demo data, so anyone can run it locally.

**10. How did you test it?**
Unit tests (`./mvnw test`) cover the business rules and the application: about 60 test methods across three test classes. We also checked the non-functional requirements against their acceptance criteria, such as 390 px phone width and the 5 MB slip limit.

**11. How is it deployed?**
Every push triggers Cloud Build, which builds a Docker image, pushes it to Artifact Registry and deploys it to Cloud Run (port 8100). Database settings come from environment variables.

**12. Who did what?**
Prepare one sentence per member before the session. Do not improvise this one.

## B. Hard questions (prepare these)

**13. Is the app secure? What about the demo passwords in the repository?**
Honest answer: passwords are hashed with BCrypt, roles are enforced, CSRF protection is on, and a tenant cannot open another room's bill (it returns 404). But the default admin password and the demo tenant passwords are visible in `application.properties` and the README for demo purposes. In real use they must be set through the environment variable `DORMHUB_ADMIN_PASSWORD`, and the database password and Gemini key are already environment-only.

**14. Can a tenant cheat with a fake or edited slip?**
The AI check looks for tampering and compares the amount, but it can be wrong. That is why we never trust it alone: doubtful slips wait for the owner, and the owner can undo a payment. A bank-API check would be the real fix, but it needs a business account, which is outside this course project.

**15. Why not a real payment gateway?**
A gateway needs a registered business and fees. PromptPay with a QR code and a slip is what Thai dorms already use, so it fits the real workflow. A gateway is a next step.

**16. What about privacy? You send data to Gemini.**
The chatbot prompt contains dorm data and the slip check sends the slip image to Google. We only send what is needed for the answer, and the key is not in the code. In production you would add tenant consent and a privacy notice, and consider hiding names and account numbers before sending.

**17. Can it scale? Cloud Run usually scales to many instances.**
Not yet. Login sessions live in memory, so we run one instance with session affinity (`--max-instances 1`). That is enough for one dorm. To scale, we would store sessions in the database or Redis (Spring Session).

**18. The database uses `ddl-auto=update`. Is that safe?**
It is fine for a course project because it creates missing tables and keeps data. A real product needs migration scripts (Flyway or Liquibase) so schema changes are controlled.

**19. What if two people book the same room, or two slips pay the same bill at once?**
Be careful here. Say what is true: bookings are requests that the owner confirms, so the owner is the checkpoint. We did not add database locking, so strict concurrent protection is a known improvement. Do not claim it exists.

**20. What if Gemini is down or slow?**
Requests time out after 40 seconds (10 seconds to connect) and the system falls back: the chatbot gives built-in answers and slips are saved as Pending. No error page appears (non-functional requirements 6 and 10).

**21. Anyone can register and anyone can send a booking request. What about spam?**
Registered accounts get the TENANT role only and see no room data until the owner confirms a rent request. The owner can reject requests. Rate limiting and CAPTCHA are not built yet and would be next.

**22. Is the laundry feature real?**
No, the laundry status is a mock-up. We say so in the SRS (the requirement is named "Check Laundry Status (Mockup)"). Say it before they ask.

**23. Why does the live site show demo data?**
The project is a course mock-up for one dorm, SJD Mansion. A data seeder creates rooms, tenants and 12 months of bills so the features can be shown. A real dorm would start empty.

**24. Your requirements say 220 rooms and 2-second page loads. Did you really test that?**
Be precise: the target is in the non-functional requirements with a test method. State what you actually measured, and if you did not measure it, say it is the acceptance criterion and the demo data set meets it in normal use.

**25. Why Spring MVC with Thymeleaf instead of a React front end?**
Server-side pages are simpler to secure and to build in one project, and they work well on phones. A separate front end would add a second project and an API layer that the course scope did not need.

**26. How is the system different from existing dorm software or a LINE group?**
It combines billing, slip checking, requests and an AI assistant in one place with role-based access, and it is built to be adapted to any dorm. A chat group has no structure, no records and no reports.

**27. How did you decide what to build first?**
We prioritised what the owner needs every month: rooms, tenants, bills and payments (High), then requests and announcements (Medium), then extras like laundry (Low). The priority column in the SRS shows this.

**28. What would you do with one more month?**
Real laundry status, push notifications for due dates, session storage for several instances, database migrations, rate limiting, concurrency control for bookings, and support for several dorms.

## C. Tips for answering

1. Answer in one or two sentences, then stop.
2. If you do not know, say "We did not build that, and here is how we would" instead of guessing.
3. Never claim a feature that is not in the code (concurrency locks, real laundry, payment gateway).
4. Send questions about someone else's part to that member by name.
