# ATHENAEUM LMS: REQUIREMENTS TRACEABILITY MATRIX

**Document Version:** 2.1 (Clean-Room Pre-CI Status Audit)  
**Baseline Specification:** Requirement Elicitation LMS Refined (Group 2)  
**Permitted Status Labels:** `VERIFIED`, `IMPLEMENTED BUT UNTESTED`, `NOT DONE`  
*Rule: Do not claim anything is verified until CI has run it against a real database.*

---

## 1. BASELINE FUNCTIONAL REQUIREMENTS (FR-01 to FR-13)

| Req ID | Requirement Statement | Code Implementation | Test Class | Status | Notes |
|---|---|---|---|---|---|
| **FR-01** | Authenticate users via email/password, restrict access based on role. | `SecurityConfig.java`<br>`CustomUserDetailsService.java` | `TenantFilterAndIdorSecurityTest.java`<br>`RouteMatrixSecurityTest.java` | `IMPLEMENTED BUT UNTESTED` | RouteMatrixSecurityTest pending CI run. |
| **FR-02** | Create, update, view, and delete book records. | `BookController.java`<br>`BookService.java` | `MetadataAndSearchTest.java`<br>`PostgreSqlFlywayValidationTest.java` | `IMPLEMENTED BUT UNTESTED` | Exercised with in-memory fakes only; DB test pending CI. |
| **FR-03** | Search and filter catalog with real-time copy availability. | `CatalogApiController.java`<br>`CatalogSearchService.java` | `MetadataAndSearchTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally against in-memory fake repositories. |
| **FR-04** | Student self-service issue of an available copy with due date. | `TransactionController.java`<br>`TransactionService.java` | `RenewalsAndPaymentsTest.java`<br>`ConcurrencyAndDataIntegrityTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally with in-memory fakes only. |
| **FR-05** | Staff-assisted issue of a book to any patron account. | `CirculationDeskController.java`<br>`TransactionService.java` | `MultiTenancyAndInventoryTest.java`<br>`RouteMatrixSecurityTest.java` | `IMPLEMENTED BUT UNTESTED` | Route security test pending CI run. |
| **FR-06** | Record return and calculate overdue fine when overdue. | `TransactionService.java`<br>`FineService.java` | `BarcodeAndPolicyTest.java`<br>`RealDatabaseBusinessRulesTest.java` | `IMPLEMENTED BUT UNTESTED` | Real database test pending CI run. |
| **FR-07** | Maintain FIFO reservation queue when copies are zero. | `ReservationController.java`<br>`ReservationService.java` | `RenewalsAndPaymentsTest.java`<br>`RealDatabaseBusinessRulesTest.java` | `IMPLEMENTED BUT UNTESTED` | Real database test pending CI run. |
| **FR-08** | Notify next waiting member in queue when returned. | `ReservationService.java`<br>`NotificationService.java` | `RenewalsAndPaymentsTest.java`<br>`RealDatabaseBusinessRulesTest.java` | `IMPLEMENTED BUT UNTESTED` | Real database test pending CI run. |
| **FR-09** | Record fine and allow staff to mark fine as paid. | `FineController.java`<br>`FineService.java` | `BarcodeAndPolicyTest.java`<br>`RenewalsAndPaymentsTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally against fake repositories. |
| **FR-10** | In-app notifications for issue, return, fine, hold events. | `NotificationController.java`<br>`NotificationService.java` | `CommunicationAndPwaTest.java`<br>`TenantFilterAndIdorSecurityTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally against fake repositories. |
| **FR-11** | Summary statistics & reports (most-borrowed, aging, fines). | `ReportController.java`<br>`ReportService.java` | `ReportsAndPrivacyTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally against fake repositories. |
| **FR-12** | Admin user management (view accounts, change role, toggle). | `AdminController.java`<br>`UserService.java` | `RouteMatrixSecurityTest.java`<br>`AccountLockoutAndRateLimiterTest.java` | `IMPLEMENTED BUT UNTESTED` | Route security test pending CI run. |
| **FR-13** | Member self-registration. | `AuthController.java`<br>`UserService.java` | `FoundationSmokeTest.java`<br>`TenantFilterAndIdorSecurityTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally against fake repositories. |

---

## 2. EXTENDED FUNCTIONAL REQUIREMENTS (FR-14+)

| Req ID | Extended Requirement Statement | Code Implementation | Test Class | Status | Notes |
|---|---|---|---|---|---|
| **FR-14** | Multi-tenant isolation at database & session layers; subdomain routing. | `TenantFilter.java`<br>`TenantContext.java` | `TenantFilterAndIdorSecurityTest.java`<br>`MultiTenancyAndInventoryTest.java` | `IMPLEMENTED BUT UNTESTED` | MockMvc standalone with fakes; real DB pending CI. |
| **FR-15** | Discrete copy-level inventory tracking (barcode, condition, shelf). | `BookCopyService.java`<br>`BookCopy.java` | `MultiTenancyAndInventoryTest.java`<br>`ConcurrencyAndDataIntegrityTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally against in-memory fakes. |
| **FR-16** | Barcode & QR code printable label generation (SVG/PNG/PDF). | `BarcodeService.java`<br>`BarcodeController.java` | `BarcodeAndPolicyTest.java` | `IMPLEMENTED BUT UNTESTED` | ZXing generation tested; physical printing untested. |
| **FR-17** | ISBN bibliographic metadata auto-fill from Open Library. | `IsbnMetadataService.java`<br>`OpenLibraryClient.java` | `MetadataAndSearchTest.java` | `IMPLEMENTED BUT UNTESTED` | Offline mock payload verified; live external API unverified. |
| **FR-18** | Membership policy tier engine (UG, PG, Faculty, Staff). | `PolicyService.java`<br>`MembershipTypeRepository.java` | `BarcodeAndPolicyTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally with fake repository. |
| **FR-19** | Loan renewal blocking engine (fines, holds, max renewals). | `TransactionService.java`<br>`PolicyService.java` | `RenewalsAndPaymentsTest.java`<br>`ConcurrencyAndDataIntegrityTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally with in-memory fakes. |
| **FR-20** | Payment ledger, Razorpay HMAC webhook, cash entry & PDF receipts. | `RazorpayPaymentGateway.java`<br>`PdfReceiptGenerator.java` | `RenewalsAndPaymentsTest.java` | `IMPLEMENTED BUT UNTESTED` | Signature logic tested; live Razorpay sandbox unexercised. |
| **FR-21** | Transactional email outbox with retries, scheduled scanners. | `EmailOutboxService.java`<br>`ScheduledCirculationJobs.java` | `CommunicationAndPwaTest.java` | `IMPLEMENTED BUT UNTESTED` | Outbox loop tested; live SMTP dispatch unexercised. |
| **FR-22** | Account lockout (5 attempts / 15 min) & per-IP login rate limiting. | `LoginSecurityService.java`<br>`LoginRateLimiterFilter.java` | `AccountLockoutAndRateLimiterTest.java` | `IMPLEMENTED BUT UNTESTED` | Unit tested with fake repository; real DB pending CI. |
| **FR-23** | Fuzzy search (pg_trgm), patron wishlist, purchase requests. | `WishlistService.java`<br>`BookRequestService.java` | `TenantFilterAndIdorSecurityTest.java`<br>`MetadataAndSearchTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally with fake repositories. |
| **FR-24** | Physical stock verification and annual stocktake audit. | `StockAuditService.java`<br>`StockAuditSessionRepository.java` | `ReportsAndPrivacyTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally with fake repositories. |
| **FR-25** | Privacy & Data Subject Rights (DPDP / GDPR data export). | `PrivacyService.java`<br>`PatronApiController.java` | `ReportsAndPrivacyTest.java`<br>`TenantFilterAndIdorSecurityTest.java` | `IMPLEMENTED BUT UNTESTED` | Tested locally with fake repositories. |
| **FR-26** | Progressive Web App manifest and service worker caching. | `sw.js`<br>`manifest.json`<br>`offline.html` | `CommunicationAndPwaTest.java` | `IMPLEMENTED BUT UNTESTED` | Asset checks pass; browser PWA audit pending. |

---

## 3. NON-FUNCTIONAL REQUIREMENTS (NFR-01 to NFR-10)

| Req ID | Category | Baseline Criteria | Implementation & Evidence | Status | Notes |
|---|---|---|---|---|---|
| **NFR-01** | Performance | 95% of operations complete within 2s under normal load. | `load-test.js` targeting p95 $\le$ 2000ms. | `NOT DONE` | Benchmark harness exists; staging run not yet executed. |
| **NFR-02** | Security | BCrypt hashing, role enforcement, CSP headers. | `SecurityConfig.java`<br>`TenantFilterAndIdorSecurityTest.java` | `IMPLEMENTED BUT UNTESTED` | Full route matrix pending CI run. |
| **NFR-03** | Reliability | Atomic transactions, zero inconsistent copy states. | Spring `@Transactional` + `@Version` optimistic locking. | `IMPLEMENTED BUT UNTESTED` | Concurrency tested in Java memory; real DB pending CI. |
| **NFR-04** | Usability | Locate and self-issue in $\le$ 3 steps. | Catalog search direct issue button. | `IMPLEMENTED BUT UNTESTED` | UI implemented; user study / session audit not conducted. |
| **NFR-05** | Scalability | Supports catalog/user growth without schema redesign. | Flyway V1, V2, V3 relational schema with indexes. | `IMPLEMENTED BUT UNTESTED` | Flyway validation pending CI execution. |
| **NFR-06** | Maintainability | Layered architecture with single responsibility. | Package structure under `com.college.library.*`. | `IMPLEMENTED BUT UNTESTED` | Architectural separation exists; static analysis pending CI. |
| **NFR-07** | Availability | Target $\ge$ 99% uptime during service hours. | Health probes at `/actuator/health` + multi-stage Docker. | `IMPLEMENTED BUT UNTESTED` | Production uptime monitoring not yet deployed. |
| **NFR-08** | Compatibility | Responsive across desktop and mobile viewports. | CSS tokens, responsive layout, barcode scanning. | `IMPLEMENTED BUT UNTESTED` | Cross-browser automated matrix pending CI Playwright run. |
| **NFR-09** | Data Integrity | Unique constraints & foreign keys at DB layer. | Flyway V1 DDL constraints. | `IMPLEMENTED BUT UNTESTED` | Flyway validation pending CI run with Testcontainers. |
| **NFR-10** | Auditability | Permanent immutable event logging for key actions. | `AuditLogService.java`<br>`audit_logs` table. | `IMPLEMENTED BUT UNTESTED` | Tested locally with fake repository; real DB pending CI. |
