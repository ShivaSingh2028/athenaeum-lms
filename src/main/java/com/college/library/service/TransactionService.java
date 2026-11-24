package com.college.library.service;

import com.college.library.entity.*;
import com.college.library.finance.FineService;
import com.college.library.policy.MembershipType;
import com.college.library.policy.PolicyService;
import com.college.library.repository.TransactionRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BookCopyService bookCopyService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final PolicyService policyService;
    private final FineService fineService;

    public TransactionService(TransactionRepository transactionRepository,
                               BookCopyService bookCopyService,
                               ReservationService reservationService,
                               NotificationService notificationService,
                               PolicyService policyService,
                               @Lazy FineService fineService) {
        this.transactionRepository = transactionRepository;
        this.bookCopyService = bookCopyService;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.policyService = policyService;
        this.fineService = fineService;
    }

    /** Issues an available physical copy of a book to a user. */
    @Transactional
    public Transaction issueBook(Book book, User user) {
        BookCopy copy = bookCopyService.findFirstAvailableCopy(book)
                .orElseThrow(() -> new IllegalStateException("No available physical copies for \"" + book.getTitle() + "\"."));
        return issueCopy(copy, user, null);
    }

    /** Issues a specific physical copy (scanned by barcode) to a user. */
    @Transactional
    public synchronized Transaction issueCopy(BookCopy copy, User user, User staff) {
        bookCopyService.markIssued(copy);

        LocalDate dueDate = policyService.calculateDueDate(user);

        Transaction t = new Transaction();
        t.setTenantId(TenantContext.getTenantId());
        t.setBookCopy(copy);
        t.setBook(copy.getBook());
        t.setUser(user);
        t.setIssuedBy(staff);
        t.setIssueDate(LocalDate.now());
        t.setDueDate(dueDate);
        t.setStatus(TransactionStatus.ISSUED);

        Transaction saved = transactionRepository.save(t);
        notificationService.notify(user, "Issued copy (" + copy.getBarcode() + ") of \"" 
                + copy.getBook().getTitle() + "\". Due back on " + saved.getDueDate() + ".");

        return saved;
    }

    /** Returns a book copy, calculates overdue fine, accrues into fine ledger, and fulfills reservation. */
    @Transactional
    public Transaction returnBook(Long transactionId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (t.getTenantId() != null && !t.getTenantId().equals(TenantContext.getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Transaction belongs to another library tenant.");
        }

        t.setReturnDate(LocalDate.now());
        t.setStatus(TransactionStatus.RETURNED);

        BigDecimal fine = policyService.calculateFine(t, LocalDate.now());
        if (fine.compareTo(BigDecimal.ZERO) > 0) {
            t.setFineAmount(fine.doubleValue());
            fineService.accrueFine(t, fine);
            notificationService.notify(t.getUser(), "\"" + t.getBook().getTitle() 
                    + "\" was returned late. Overdue fine assessed: ₹" + fine);
        }

        Transaction saved = transactionRepository.save(t);
        if (t.getBookCopy() != null) {
            bookCopyService.markReturned(t.getBookCopy(), CopyCondition.GOOD);
            reservationService.fulfillNextReservation(t.getBookCopy().getBook());
        }

        return saved;
    }

    /** Patron self-return with strict ownership and tenant isolation check. */
    @Transactional
    public Transaction selfReturnBook(Long transactionId, User user) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (t.getTenantId() != null && !t.getTenantId().equals(TenantContext.getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Transaction belongs to another library tenant.");
        }
        if (!user.getRole().isStaffOrAdmin() && !t.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only return books borrowed by your own account.");
        }
        return returnBook(transactionId);
    }

    /** Patron-initiated or staff-assisted renewal enforcing all blocking rules. */
    @Transactional
    public synchronized Transaction renewLoan(Long transactionId, User user) {
        Transaction t = findById(transactionId);

        // Rule 0: Tenant validation
        if (t.getTenantId() != null && !t.getTenantId().equals(TenantContext.getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Transaction belongs to another library tenant.");
        }

        // Rule 1: Ownership validation
        if (!user.getRole().isStaffOrAdmin() && !t.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only renew loans issued to your own account.");
        }

        // Rule 2: Active loan status check
        if (t.getStatus() != TransactionStatus.ISSUED) {
            throw new IllegalStateException("Cannot renew a transaction that is already " + t.getStatus());
        }

        // Rule 3: Policy renewal limit check
        MembershipType policy = policyService.getPolicyForUser(t.getUser());
        if (t.getRenewalCount() >= policy.getRenewalLimit()) {
            throw new IllegalStateException("Renewal limit reached. Maximum renewals permitted: " + policy.getRenewalLimit());
        }

        // Rule 4: Holding reservation check (someone is waiting in FIFO queue)
        if (reservationService.hasPendingReservation(t.getBook())) {
            throw new IllegalStateException("Renewal blocked: Another library member has reserved this title.");
        }

        // Rule 5: Fine threshold check (unpaid fines > ₹100 blocks renewal)
        BigDecimal unpaidFine = fineService.getUserUnpaidFineTotal(t.getUser());
        BigDecimal fineThreshold = new BigDecimal("100.00");
        if (unpaidFine.compareTo(fineThreshold) > 0) {
            throw new IllegalStateException("Renewal blocked: Outstanding unpaid fines of ₹" + unpaidFine 
                    + " exceed the ₹" + fineThreshold + " threshold. Please settle fines first.");
        }

        // Apply renewal
        t.setRenewalCount(t.getRenewalCount() + 1);
        t.setDueDate(t.getDueDate().plusDays(policy.getLoanDurationDays()));
        Transaction saved = transactionRepository.save(t);

        notificationService.notify(t.getUser(), "Renewed \"" + t.getBook().getTitle() 
                + "\". New due date: " + saved.getDueDate() + " (Renewal " + t.getRenewalCount() 
                + "/" + policy.getRenewalLimit() + ")");

        return saved;
    }

    public List<Transaction> findByUser(User user) {
        return transactionRepository.findByTenantIdAndUserOrderByIssueDateDesc(TenantContext.getTenantId(), user);
    }

    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    public List<Transaction> findAllIssued() {
        return transactionRepository.findByTenantIdAndStatus(TenantContext.getTenantId(), TransactionStatus.ISSUED);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findByTenantIdOrderByIssueDateDesc(TenantContext.getTenantId());
    }

    public List<Transaction> findOverdue() {
        return transactionRepository.findByTenantIdAndStatusAndDueDateBefore(
                TenantContext.getTenantId(), TransactionStatus.ISSUED, LocalDate.now());
    }

    public boolean hasActiveLoan(Long bookId, User user) {
        return transactionRepository.findByTenantIdAndBookIdAndStatus(
                TenantContext.getTenantId(), bookId, TransactionStatus.ISSUED)
                .stream().anyMatch(t -> t.getUser().getId().equals(user.getId()));
    }

    public long countIssued() {
        return transactionRepository.countByTenantIdAndStatus(TenantContext.getTenantId(), TransactionStatus.ISSUED);
    }

    public long countReturned() {
        return transactionRepository.countByTenantIdAndStatus(TenantContext.getTenantId(), TransactionStatus.RETURNED);
    }

    public double totalFinesCollected() {
        return fineService.getTotalFinesCollected().doubleValue();
    }

    public double totalFinesOutstanding() {
        return fineService.getTotalFinesOutstanding().doubleValue();
    }

    @Transactional
    public void markFinePaid(Long transactionId) {
        Transaction t = findById(transactionId);
        t.setFinePaid(true);
        transactionRepository.save(t);
    }
}
