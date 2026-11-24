package com.college.library.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class EmailOutboxService {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);

    private final EmailOutboxRepository outboxRepository;
    private final EmailSender emailSender;

    public EmailOutboxService(EmailOutboxRepository outboxRepository, EmailSender emailSender) {
        this.outboxRepository = outboxRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public EmailOutbox queueEmail(String tenantId, String recipientEmail, String subject, String bodyHtml) {
        EmailOutbox outbox = new EmailOutbox(tenantId, recipientEmail.trim(), subject, bodyHtml);
        return outboxRepository.save(outbox);
    }

    @Transactional
    public int processOutbox(int batchSize) {
        List<EmailOutbox> pending = outboxRepository.findPendingEmails(EmailStatus.PENDING, PageRequest.of(0, batchSize));
        int sentCount = 0;

        for (EmailOutbox item : pending) {
            try {
                emailSender.send(item.getRecipientEmail(), item.getSubject(), item.getBodyHtml());
                item.setStatus(EmailStatus.SENT);
                item.setSentAt(OffsetDateTime.now());
                item.setLastError(null);
                sentCount++;
            } catch (Exception e) {
                log.warn("Failed to dispatch email {} to {}: {}", item.getId(), item.getRecipientEmail(), e.getMessage());
                item.setRetryCount(item.getRetryCount() + 1);
                item.setLastError(e.getMessage());
                if (item.getRetryCount() >= item.getMaxRetries()) {
                    item.setStatus(EmailStatus.FAILED);
                }
            }
            outboxRepository.save(item);
        }

        return sentCount;
    }

    // --- High-Level Notification Templates ---

    public EmailOutbox sendWelcomeEmail(String tenantId, String recipientEmail, String userName, String memberCode) {
        String subject = "Welcome to Athenaeum Library";
        String body = String.format("""
                <div style="font-family:sans-serif; max-width:600px; margin:auto; border:1px solid #e2e8f0; border-radius:8px; padding:24px;">
                    <h2 style="color:#1B2A4A; margin-top:0;">Welcome to Athenaeum Library, %s!</h2>
                    <p>Your library patron membership account has been established successfully.</p>
                    <div style="background:#f8fafc; padding:16px; border-left:4px solid #B98D2E; border-radius:4px; margin:20px 0;">
                        <strong>Your Patron Membership Code:</strong> <code style="font-size:1.1rem; color:#1B2A4A;">%s</code>
                    </div>
                    <p>You can now browse the catalog, reserve books, and manage loans online.</p>
                    <p style="font-size:0.85rem; color:#64748b; margin-top:24px;">Athenaeum Library Management Platform</p>
                </div>
                """, userName, memberCode != null ? memberCode : "N/A");
        return queueEmail(tenantId, recipientEmail, subject, body);
    }

    public EmailOutbox sendDueDateReminderEmail(String tenantId, String recipientEmail, String bookTitle, LocalDate dueDate, int daysRemaining) {
        String subject = String.format("Reminder: '%s' is due in %d day(s)", bookTitle, daysRemaining);
        String body = String.format("""
                <div style="font-family:sans-serif; max-width:600px; margin:auto; border:1px solid #e2e8f0; border-radius:8px; padding:24px;">
                    <h2 style="color:#1B2A4A; margin-top:0;">Upcoming Book Due Date Reminder</h2>
                    <p>Please be advised that your borrowed book <strong>%s</strong> is scheduled for return on:</p>
                    <div style="background:#fefce8; padding:16px; border-left:4px solid #ca8a04; border-radius:4px; margin:20px 0;">
                        <strong>Due Date:</strong> %s (%d day(s) remaining)
                    </div>
                    <p>You may renew this item online through your dashboard if no pending reservations exist.</p>
                    <p style="font-size:0.85rem; color:#64748b; margin-top:24px;">Athenaeum Automated Circulation</p>
                </div>
                """, bookTitle, dueDate, daysRemaining);
        return queueEmail(tenantId, recipientEmail, subject, body);
    }

    public EmailOutbox sendOverdueNoticeEmail(String tenantId, String recipientEmail, String bookTitle, LocalDate dueDate, BigDecimal accruedFine) {
        String subject = String.format("URGENT: Overdue Notice for '%s'", bookTitle);
        String body = String.format("""
                <div style="font-family:sans-serif; max-width:600px; margin:auto; border:1px solid #fecaca; border-radius:8px; padding:24px;">
                    <h2 style="color:#7A1F2B; margin-top:0;">Library Overdue Notice</h2>
                    <p>The following book issued to your account has exceeded its designated loan period:</p>
                    <div style="background:#fff1f2; padding:16px; border-left:4px solid #7A1F2B; border-radius:4px; margin:20px 0;">
                        <strong>Book Title:</strong> %s<br>
                        <strong>Due Date:</strong> %s<br>
                        <strong>Current Accrued Fine:</strong> ₹%.2f
                    </div>
                    <p>Please return the copy to the library front desk immediately to prevent additional fine accrual.</p>
                    <p style="font-size:0.85rem; color:#64748b; margin-top:24px;">Athenaeum Circulation Department</p>
                </div>
                """, bookTitle, dueDate, accruedFine.doubleValue());
        return queueEmail(tenantId, recipientEmail, subject, body);
    }

    public EmailOutbox sendReservationReadyEmail(String tenantId, String recipientEmail, String bookTitle, String pickupLocation, LocalDate holdExpiryDate) {
        String subject = String.format("Hold Ready: '%s' is waiting for pickup", bookTitle);
        String body = String.format("""
                <div style="font-family:sans-serif; max-width:600px; margin:auto; border:1px solid #bbf7d0; border-radius:8px; padding:24px;">
                    <h2 style="color:#1B2A4A; margin-top:0;">Your Reserved Book is Ready!</h2>
                    <p>Great news! The hold request for <strong>%s</strong> has been fulfilled and is held at the circulation desk:</p>
                    <div style="background:#f0fdf4; padding:16px; border-left:4px solid #16a34a; border-radius:4px; margin:20px 0;">
                        <strong>Pickup Shelf:</strong> %s<br>
                        <strong>Hold Valid Until:</strong> %s
                    </div>
                    <p>Please bring your digital or physical library card to claim the book.</p>
                    <p style="font-size:0.85rem; color:#64748b; margin-top:24px;">Athenaeum Circulation Desk</p>
                </div>
                """, bookTitle, pickupLocation, holdExpiryDate);
        return queueEmail(tenantId, recipientEmail, subject, body);
    }

    public EmailOutbox sendFineReceiptEmail(String tenantId, String recipientEmail, BigDecimal amountPaid, String paymentMethod, String receiptNumber) {
        String subject = "Library Payment Receipt #" + receiptNumber;
        String body = String.format("""
                <div style="font-family:sans-serif; max-width:600px; margin:auto; border:1px solid #e2e8f0; border-radius:8px; padding:24px;">
                    <h2 style="color:#1B2A4A; margin-top:0;">Payment Receipt Confirmation</h2>
                    <p>Thank you for settling your library account balance. Details of the transaction:</p>
                    <div style="background:#f8fafc; padding:16px; border-left:4px solid #B98D2E; border-radius:4px; margin:20px 0;">
                        <strong>Receipt Reference:</strong> %s<br>
                        <strong>Amount Paid:</strong> ₹%.2f<br>
                        <strong>Payment Method:</strong> %s<br>
                        <strong>Date:</strong> %s
                    </div>
                    <p>Your circulation privileges remain in good standing.</p>
                    <p style="font-size:0.85rem; color:#64748b; margin-top:24px;">Athenaeum Finance & Circulation</p>
                </div>
                """, receiptNumber, amountPaid.doubleValue(), paymentMethod, LocalDate.now());
        return queueEmail(tenantId, recipientEmail, subject, body);
    }
}
