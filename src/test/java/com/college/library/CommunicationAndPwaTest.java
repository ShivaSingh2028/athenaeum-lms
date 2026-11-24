package com.college.library;

import com.college.library.communication.*;
import com.college.library.entity.*;
import com.college.library.fake.*;
import com.college.library.finance.Fine;
import com.college.library.finance.FineRepository;
import com.college.library.finance.FineService;
import com.college.library.policy.PolicyService;
import com.college.library.scheduling.ScheduledCirculationJobs;
import com.college.library.service.NotificationService;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

public class CommunicationAndPwaTest {

    private static final String TENANT_ID = "t-i18n-univ";

    private FakeEmailOutboxRepository outboxRepo;
    private FakeNotificationRepository notificationRepo;
    private FakeTransactionRepository transactionRepo;
    private FakeReservationRepository reservationRepo;
    private FakeBookCopyRepository copyRepo;
    private FakeMembershipTypeRepository membershipRepo;
    private FakeFineRepository fineRepo;
    private FakePaymentRepository paymentRepo;

    private MockEmailSender mockEmailSender;
    private EmailOutboxService emailOutboxService;
    private PolicyService policyService;
    private FineService fineService;
    private NotificationService notificationService;
    private ScheduledCirculationJobs circulationJobs;

    private User samplePatron;
    private BookCopy sampleCopy;

    @BeforeEach
    public void setup() {
        TenantContext.setTenantId(TENANT_ID);

        outboxRepo = new FakeEmailOutboxRepository();
        notificationRepo = new FakeNotificationRepository();
        transactionRepo = new FakeTransactionRepository();
        reservationRepo = new FakeReservationRepository();
        copyRepo = new FakeBookCopyRepository();
        membershipRepo = new FakeMembershipTypeRepository();
        fineRepo = new FakeFineRepository();
        paymentRepo = new FakePaymentRepository();

        mockEmailSender = new MockEmailSender();
        emailOutboxService = new EmailOutboxService(outboxRepo, mockEmailSender);
        policyService = new PolicyService(membershipRepo);
        fineService = new FineService(fineRepo, paymentRepo);
        notificationService = new NotificationService(notificationRepo);

        circulationJobs = new ScheduledCirculationJobs(
                transactionRepo,
                reservationRepo,
                copyRepo,
                policyService,
                fineService,
                emailOutboxService,
                notificationRepo
        );

        samplePatron = new User("Nikola Tesla", "tesla@example.edu", "pass", Role.MEMBER);
        samplePatron.setId(501L);
        samplePatron.setTenantId(TENANT_ID);

        Book book = new Book();
        book.setId(10L);
        book.setTitle("Principles of Electromagnetism");
        book.setTenantId(TENANT_ID);

        sampleCopy = new BookCopy(book, "BC-TESLA-001", "Lab Rack 1", CopyCondition.GOOD);
        sampleCopy.setId(201L);
        sampleCopy.setTenantId(TENANT_ID);
        copyRepo.save(sampleCopy);
    }

    @AfterEach
    public void teardown() {
        TenantContext.clear();
    }

    @Test
    public void testEmailOutboxQueuingAndDispatch() {
        // Queue emails
        emailOutboxService.sendWelcomeEmail(TENANT_ID, "tesla@example.edu", "Nikola Tesla", "LIB-7890");
        emailOutboxService.sendDueDateReminderEmail(TENANT_ID, "tesla@example.edu", "Principles of Electromagnetism", LocalDate.now().plusDays(3), 3);

        assertEquals(2, outboxRepo.count());

        // Process Outbox
        int dispatched = emailOutboxService.processOutbox(10);
        assertEquals(2, dispatched);
        assertEquals(2, mockEmailSender.getSentMessages().size());

        // Verify sent status
        EmailOutbox first = outboxRepo.findAll().get(0);
        assertEquals(EmailStatus.SENT, first.getStatus());
        assertNotNull(first.getSentAt());
        assertNull(first.getLastError());
    }

    @Test
    public void testEmailOutboxRetryAndFailureHandling() {
        // Provide failing sender
        EmailSender failingSender = (recipient, subject, bodyHtml) -> {
            throw new RuntimeException("SMTP Connection Refused");
        };
        EmailOutboxService serviceWithFailures = new EmailOutboxService(outboxRepo, failingSender);

        EmailOutbox queued = serviceWithFailures.queueEmail(TENANT_ID, "err@example.edu", "Test Subject", "<p>Error</p>");

        // 1st attempt
        serviceWithFailures.processOutbox(10);
        EmailOutbox item = outboxRepo.findById(queued.getId()).orElseThrow();
        assertEquals(1, item.getRetryCount());
        assertEquals(EmailStatus.PENDING, item.getStatus());
        assertTrue(item.getLastError().contains("SMTP Connection Refused"));

        // 2nd and 3rd attempts
        serviceWithFailures.processOutbox(10);
        serviceWithFailures.processOutbox(10);

        item = outboxRepo.findById(queued.getId()).orElseThrow();
        assertEquals(3, item.getRetryCount());
        assertEquals(EmailStatus.FAILED, item.getStatus(), "Should transition to FAILED after max retries");
    }

    @Test
    public void testScheduledOverdueLoansDetectionAndFineAccrual() {
        // Create an overdue loan: 5 days past due
        LocalDate pastDueDate = LocalDate.now().minusDays(5);
        Transaction tx = new Transaction(sampleCopy, samplePatron, LocalDate.now().minusDays(20), pastDueDate);
        tx.setId(301L);
        tx.setTenantId(TENANT_ID);
        tx.setStatus(TransactionStatus.ISSUED);
        transactionRepo.save(tx);

        int processed = circulationJobs.processOverdueLoans();
        assertEquals(1, processed);

        // Transaction status should be OVERDUE
        Transaction updatedTx = transactionRepo.findById(301L).orElseThrow();
        assertEquals(TransactionStatus.OVERDUE, updatedTx.getStatus());

        // Fine should have been accrued
        assertEquals(1, fineRepo.count());
        Fine fine = fineRepo.findAll().get(0);
        assertTrue(fine.getAmount().compareTo(BigDecimal.ZERO) > 0);

        // In-app notification and email should have been sent
        assertEquals(1, notificationRepo.count());
        assertEquals(1, outboxRepo.count());
        assertTrue(outboxRepo.findAll().get(0).getSubject().contains("URGENT: Overdue Notice"));
    }

    @Test
    public void testScheduledDueSoonReminders() {
        // Create a loan due in exactly 3 days
        LocalDate threeDaysAway = LocalDate.now().plusDays(3);
        Transaction tx = new Transaction(sampleCopy, samplePatron, LocalDate.now().minusDays(10), threeDaysAway);
        tx.setId(302L);
        tx.setTenantId(TENANT_ID);
        tx.setStatus(TransactionStatus.ISSUED);
        transactionRepo.save(tx);

        int count = circulationJobs.processDueSoonReminders();
        assertEquals(1, count);

        assertEquals(1, notificationRepo.count());
        assertTrue(notificationRepo.findAll().get(0).getTitle().contains("3 day(s) left"));

        assertEquals(1, outboxRepo.count());
        assertTrue(outboxRepo.findAll().get(0).getSubject().contains("due in 3 day(s)"));
    }

    @Test
    public void testNotificationCenterMarkAsReadAndUnreadCount() {
        Notification n1 = new Notification(samplePatron, "Notice 1", "Message 1", "CIRCULATION");
        Notification n2 = new Notification(samplePatron, "Notice 2", "Message 2", "HOLD");
        n1.setTenantId(TENANT_ID);
        n2.setTenantId(TENANT_ID);
        notificationRepo.save(n1);
        notificationRepo.save(n2);

        assertEquals(2, notificationService.getUnreadCount(samplePatron));

        notificationService.markAsRead(n1.getId());
        assertEquals(1, notificationService.getUnreadCount(samplePatron));

        notificationService.markAllAsRead(samplePatron);
        assertEquals(0, notificationService.getUnreadCount(samplePatron));
    }

    @Test
    public void testI18nMessageBundles() {
        ResourceBundle enBundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        ResourceBundle hiBundle = ResourceBundle.getBundle("messages", new Locale("hi"));

        assertEquals("Dashboard", enBundle.getString("nav.dashboard"));
        assertEquals("डैशबोर्ड", hiBundle.getString("nav.dashboard"));

        assertEquals("Discovery", enBundle.getString("nav.discovery"));
        assertEquals("खोज एवं संग्रह", hiBundle.getString("nav.discovery"));

        assertEquals("Borrow", enBundle.getString("catalog.borrow"));
        assertEquals("उधार लें", hiBundle.getString("catalog.borrow"));
    }
}
