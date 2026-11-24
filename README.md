# Advanced Library Management System

A full-stack library management system built with **Spring Boot 3**, **Spring Security**, **Spring Data JPA**, **Thymeleaf**, and **MySQL**.

## Features

- **Role-based access**: Admin, Librarian, Student — each with their own dashboard and permissions
- **Book catalog**: add/edit/delete books, search by title/author/ISBN, copy tracking
- **Issue & Return**: students self-issue available books; librarians can issue at the desk
- **Fines**: automatic overdue fine calculation on return (configurable rate)
- **Reservations**: students can reserve a book that's fully checked out; the next person in line is automatically notified when a copy is returned
- **Notifications**: in-app alerts for issues, returns, fines, and reservation availability
- **Admin reports**: most-borrowed books, currently overdue list, fines collected vs outstanding

## Prerequisites

- JDK 17+
- Maven 3.8+ (or use an IDE that bundles it, like IntelliJ)
- MySQL Server + MySQL Workbench (or any MySQL client)

## Setup

### 1. Create the database
In MySQL Workbench, run:
```sql
CREATE DATABASE library_db;
```
(You've likely already done this.)

### 2. Configure the connection
Open `src/main/resources/application.properties` and update:
```properties
spring.datasource.username=root
spring.datasource.password=CHANGE_ME
```
to match your MySQL Workbench login.

### 3. Run the app
**From IntelliJ IDEA:** open the project folder, let Maven sync, then run `LibraryManagementApplication.java`.

**From terminal:**
```bash
mvn spring-boot:run
```

Hibernate will automatically create all tables in `library_db` on first run (`spring.jpa.hibernate.ddl-auto=update`), and demo data will be seeded automatically.

### 4. Open in browser
Visit **http://localhost:8080**

## Demo accounts (auto-created on first run)

| Role      | Email                  | Password    |
|-----------|-------------------------|-------------|
| Admin     | admin@college.edu       | admin123    |
| Librarian | librarian@college.edu   | lib123      |
| Student   | student@college.edu     | student123  |

New students can also self-register via the "Create an account" link on the login page.

## Project structure

```
com.college.library
├── entity/        JPA entities: User, Book, Transaction, Reservation, Notification
├── repository/    Spring Data JPA repositories
├── service/       Business logic (issue/return, fines, reservations, reports)
├── controller/     MVC controllers serving Thymeleaf pages
├── config/        Spring Security configuration
└── resources/
    ├── templates/  Thymeleaf HTML pages
    └── static/css/ Stylesheet
```

## Configurable business rules

In `application.properties`:
```properties
library.loan.period-days=14   # how many days a book can be borrowed
library.fine.per-day=5        # fine per day overdue (in ₹)
library.reservation.hold-days=2  # days a reserved book is held before expiring
```

## Switching back to H2 (no MySQL needed)

If you want to quickly demo without MySQL, replace the datasource block in `application.properties` with:
```properties
spring.datasource.url=jdbc:h2:mem:librarydb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```
The H2 driver is already included as a dependency in `pom.xml`.

## Known Limitations & Untested Areas

This project adheres to strict honesty criteria regarding operational readiness:

| Capability | Status | Notes |
|---|---|---|
| **Live Razorpay Sandbox** | `IMPLEMENTED BUT UNTESTED` | HMAC-SHA256 signature verification and order creation implemented; live webhook roundtrips against external sandbox servers not executed. |
| **Live SMTP Email Delivery** | `IMPLEMENTED BUT UNTESTED` | Asynchronous retry outbox queue and templates implemented; live dispatch through external third-party SMTP servers unverified. |
| **Performance Benchmark (k6)** | `IMPLEMENTED BUT UNTESTED` | Script `load-test.js` configured with p95 <= 2.0s threshold; pending staging run. |
| **Playwright E2E Browser Suite** | `IMPLEMENTED BUT UNTESTED` | Test suites created in `e2e/library-flows.spec.ts` covering Member, Librarian, and Admin journeys with screenshot captures; not run in live browser. |
| **Testcontainers PostgreSQL Integration** | `IMPLEMENTED BUT UNTESTED` | Test classes written (`PostgreSqlFlywayValidationTest`, `RealDatabaseBusinessRulesTest`); pending first run on Ubuntu GitHub Actions runner. |
| **Route Matrix Spring Security Tests** | `IMPLEMENTED BUT UNTESTED` | Test class `RouteMatrixSecurityTest` written; pending execution in CI. |
| **Web Push Notifications** | `NOT DONE` | In-app notification center is active; Web Push (VAPID) is not implemented. |
| **Sentry / Error Tracking** | `NOT DONE` | Sentry SDK not integrated into the runtime application build. |
| **Automated Backups & Disaster Recovery** | `IMPLEMENTED BUT UNTESTED` | Documented and reproducible in `RUNBOOK_BACKUP_RESTORE.md` with SHA-256 checksums and verification queries; live drill not run. |
| **OpenAPI / Swagger UI** | `IMPLEMENTED BUT UNTESTED` | Added to Maven `ci` profile for automated generation in CI. |
| **Hindi i18n Translation** | `NOT DONE` | Core navigation labels exist; comprehensive domain translation is not done (~20% partial). |
| **Staff TOTP 2FA Challenge** | `NOT DONE` | Database columns and QR secret generation exist; interactive login challenge filter in Spring Security is not implemented. |


