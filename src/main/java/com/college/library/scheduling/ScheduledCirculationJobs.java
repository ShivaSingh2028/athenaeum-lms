package com.college.library.scheduling;

import com.college.library.communication.EmailOutboxService;
import com.college.library.entity.*;
import com.college.library.finance.FineService;
import com.college.library.policy.PolicyService;
import com.college.library.repository.BookCopyRepository;
import com.college.library.repository.NotificationRepository;
import com.college.library.repository.ReservationRepository;
import com.college.library.repository.TransactionRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class ScheduledCirculationJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCirculationJobs.class);

    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final BookCopyRepository copyRepository;
    private final PolicyService policyService;
    private final FineService fineService;
    private final EmailOutboxService emailOutboxService;
    private final NotificationRepository notificationRepository;

    public ScheduledCirculationJobs(TransactionRepository transactionRepository,
                                    ReservationRepository reservationRepository,
                                    BookCopyRepository copyRepository,
                                    PolicyService policyService,
                                    FineService fineService,
                                    EmailOutboxService emailOutboxService,
                                    NotificationRepository notificationRepository) {
        this.transactionRepository = transactionRepository;
        this.reservationRepository = reservationRepository;
        this.copyRepository = copyRepository;
        this.policyService = policyService;
        this.fineService = fineService;
        this.emailOutboxService = emailOutboxService;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Nightly job: Scan active loans, mark overdue loans, accrue daily fines, and notify patrons.
     */
    @Scheduled(cron = "${app.scheduling.overdue-cron:0 0 1 * * ?}")
    @SchedulerLock(name = "processOverdueLoans", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    @Transactional
    public int processOverdueLoans() {
        log.info("[SCHEDULED] Running overdue loans scanner...");
        LocalDate today = LocalDate.now();
        List<Transaction> overdueLoans = transactionRepository.findByStatusAndDueDateBefore(TransactionStatus.ISSUED, today);

        int processed = 0;
        for (Transaction tx : overdueLoans) {
            tx.setStatus(TransactionStatus.OVERDUE);
            transactionRepository.save(tx);

            BigDecimal fineAmount = policyService.calculateFine(tx, today);
            if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                fineService.accrueFine(tx, fineAmount);
            }

            // Notification
            String bookTitle = tx.getBookCopy() != null && tx.getBookCopy().getBook() != null
                    ? tx.getBookCopy().getBook().getTitle()
                    : "Borrowed Book";

            Notification notification = new Notification(
                    tx.getUser(),
                    "Overdue Book Notice",
                    String.format("'%s' was due on %s. Please return it immediately to avoid escalating fines.", bookTitle, tx.getDueDate()),
                    "OVERDUE"
            );
            notificationRepository.save(notification);

            // Email Outbox
            emailOutboxService.sendOverdueNoticeEmail(
                    tx.getTenantId(),
                    tx.getUser().getEmail(),
                    bookTitle,
                    tx.getDueDate(),
                    fineAmount
            );

            processed++;
        }

        log.info("[SCHEDULED] Completed overdue loans check: {} records processed", processed);
        return processed;
    }

    /**
     * Daily morning job: Send due-date reminders (3 days and 1 day in advance).
     */
    @Scheduled(cron = "${app.scheduling.reminders-cron:0 0 7 * * ?}")
    @SchedulerLock(name = "processDueSoonReminders", lockAtMostFor = "10m", lockAtLeastFor = "30s")
    public int processDueSoonReminders() {
        log.info("[SCHEDULED] Sending upcoming due-date reminders...");
        LocalDate today = LocalDate.now();
        List<Transaction> activeLoans = transactionRepository.findByStatus(TransactionStatus.ISSUED);

        int count = 0;
        for (Transaction tx : activeLoans) {
            if (tx.getDueDate() == null) continue;

            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, tx.getDueDate());
            if (daysUntilDue == 3 || daysUntilDue == 1) {
                String title = tx.getBookCopy() != null && tx.getBookCopy().getBook() != null
                        ? tx.getBookCopy().getBook().getTitle()
                        : "Library Book";

                Notification n = new Notification(
                        tx.getUser(),
                        "Due Date Reminder (" + daysUntilDue + " day(s) left)",
                        "Your loan for '" + title + "' is due on " + tx.getDueDate() + ".",
                        "CIRCULATION"
                );
                notificationRepository.save(n);

                emailOutboxService.sendDueDateReminderEmail(
                        tx.getTenantId(),
                        tx.getUser().getEmail(),
                        title,
                        tx.getDueDate(),
                        (int) daysUntilDue
                );
                count++;
            }
        }
        return count;
    }

    /**
     * Periodic job: Dispatch pending transactional emails from the outbox table.
     */
    @Scheduled(fixedDelayString = "${app.scheduling.outbox-delay-ms:30000}", initialDelay = 10000)
    @SchedulerLock(name = "dispatchEmailOutbox", lockAtMostFor = "5m", lockAtLeastFor = "5s")
    public int dispatchEmailOutbox() {
        return emailOutboxService.processOutbox(50);
    }
}
