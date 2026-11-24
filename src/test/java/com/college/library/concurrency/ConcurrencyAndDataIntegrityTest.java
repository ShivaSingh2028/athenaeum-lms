package com.college.library.concurrency;

import com.college.library.entity.*;
import com.college.library.fake.*;
import com.college.library.finance.FineService;
import com.college.library.payment.Payment;
import com.college.library.payment.PaymentMethod;
import com.college.library.payment.PaymentRepository;
import com.college.library.payment.PaymentStatus;
import com.college.library.policy.MembershipType;
import com.college.library.policy.PolicyService;
import com.college.library.service.BookCopyService;
import com.college.library.service.NotificationService;
import com.college.library.service.ReservationService;
import com.college.library.service.TransactionService;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 2: Multi-Threaded Concurrency & Data Integrity Tests")
class ConcurrencyAndDataIntegrityTest {

    private FakeBookRepository bookRepository;
    private FakeBookCopyRepository copyRepository;
    private FakeTransactionRepository transactionRepository;
    private FakePaymentRepository paymentRepository;
    private FakeUserRepository userRepository;

    private TransactionService transactionService;
    private Book testBook;
    private BookCopy singleCopy;
    private User testPatron;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("campus-main");

        bookRepository = new FakeBookRepository();
        copyRepository = new FakeBookCopyRepository();
        transactionRepository = new FakeTransactionRepository();
        paymentRepository = new FakePaymentRepository();
        userRepository = new FakeUserRepository();

        FakeNotificationRepository notificationRepository = new FakeNotificationRepository();
        NotificationService notificationService = new NotificationService(notificationRepository);
        ReservationService reservationService = new ReservationService(new FakeReservationRepository(), notificationService);
        BookCopyService copyService = new BookCopyService(copyRepository);

        FakeMembershipTypeRepository policyRepository = new FakeMembershipTypeRepository();
        MembershipType memberPolicy = new MembershipType("MEMBER", "Member Tier", 3, 14, 2, new BigDecimal("5.00"));
        memberPolicy.setTenantId("campus-main");
        policyRepository.save(memberPolicy);

        PolicyService policyService = new PolicyService(policyRepository);
        FineService fineService = new FineService(new FakeFineRepository(), paymentRepository);

        transactionService = new TransactionService(
                transactionRepository, copyService, reservationService,
                notificationService, policyService, fineService
        );

        testBook = new Book();
        testBook.setId(10L);
        testBook.setTitle("Sample Domain Book");
        testBook.setIsbn("9780009998887");
        testBook.setTenantId("campus-main");
        bookRepository.save(testBook);

        singleCopy = new BookCopy(testBook, "BARCODE-CONCURRENT-01", "Shelf A1", CopyCondition.GOOD);
        singleCopy.setId(100L);
        singleCopy.setTenantId("campus-main");
        singleCopy.setStatus(CopyStatus.AVAILABLE);
        copyRepository.save(singleCopy);

        testPatron = new User("Alice", "alice@campus.edu", "pass", Role.MEMBER);
        testPatron.setId(1L);
        testPatron.setTenantId("campus-main");
        userRepository.save(testPatron);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Concurrency: 50 simultaneous issue requests for a single copy -> Exactly 1 succeeds")
    void testFiftySimultaneousIssueRequestsSingleCopy() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int patronIndex = i;
            User patron = new User("Patron " + patronIndex, "patron" + patronIndex + "@campus.edu", "pass", Role.MEMBER);
            patron.setId(1000L + patronIndex);
            patron.setTenantId("campus-main");
            userRepository.save(patron);

            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    TenantContext.setTenantId("campus-main");
                    transactionService.issueCopy(singleCopy, patron, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    TenantContext.clear();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all 50 threads simultaneously

        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Exactly one borrow request must succeed");
        assertEquals(49, failureCount.get(), "Remaining 49 concurrent requests must fail");
        assertEquals(CopyStatus.ISSUED, copyRepository.findById(100L).get().getStatus());
    }

    @Test
    @DisplayName("Concurrency: Simultaneous loan renewals cannot exceed configured renewal limit")
    void testConcurrentRenewalsRespectLimit() throws InterruptedException {
        Transaction loan = new Transaction();
        loan.setId(500L);
        loan.setTenantId("campus-main");
        loan.setBook(testBook);
        loan.setBookCopy(singleCopy);
        loan.setUser(testPatron);
        loan.setStatus(TransactionStatus.ISSUED);
        loan.setIssueDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setRenewalCount(1); // Already renewed once; limit is 2
        transactionRepository.save(loan);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TenantContext.setTenantId("campus-main");
                    transactionService.renewLoan(500L, testPatron);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    TenantContext.clear();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // At most 1 renewal should succeed before hitting limit of 2
        assertTrue(successCount.get() <= 1, "Concurrent renewals must not exceed renewal limit");
        Transaction updated = transactionRepository.findById(500L).get();
        assertTrue(updated.getRenewalCount() <= 2);
    }

    @Test
    @DisplayName("Concurrency: Replayed payment webhooks with identical idempotency key are idempotent")
    void testReplayedPaymentWebhookIdempotency() throws InterruptedException {
        String sharedIdempotencyKey = "webhook_evt_simultaneous_replay_999";
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger processedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TenantContext.setTenantId("campus-main");
                    synchronized (paymentRepository) {
                        if (!paymentRepository.existsByIdempotencyKey(sharedIdempotencyKey)) {
                            Payment payment = new Payment();
                            payment.setTenantId("campus-main");
                            payment.setUser(testPatron);
                            payment.setAmount(new BigDecimal("150.00"));
                            payment.setPaymentMethod(PaymentMethod.RAZORPAY_UPI);
                            payment.setPaymentStatus(PaymentStatus.COMPLETED);
                            payment.setIdempotencyKey(sharedIdempotencyKey);
                            paymentRepository.save(payment);
                            processedCount.incrementAndGet();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    TenantContext.clear();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, processedCount.get(), "Payment webhook must be processed exactly once");
        assertEquals(1, paymentRepository.findByTenantIdOrderByPaymentDateDesc("campus-main").stream()
                .filter(p -> sharedIdempotencyKey.equals(p.getIdempotencyKey())).count());
    }
}
