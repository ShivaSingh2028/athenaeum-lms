package com.college.library.service;

import com.college.library.entity.*;
import com.college.library.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BookService bookService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;

    @Value("${library.loan.period-days:14}")
    private int loanPeriodDays;

    @Value("${library.fine.per-day:5}")
    private double finePerDay;

    public TransactionService(TransactionRepository transactionRepository,
                               BookService bookService,
                               ReservationService reservationService,
                               NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.bookService = bookService;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
    }

    /** Issues a book copy to a user, provided at least one copy is available. */
    public Transaction issueBook(Book book, User user) {
        if (!book.isAvailable()) {
            throw new IllegalStateException("No copies of \"" + book.getTitle() + "\" are currently available. "
                    + "You can place a reservation instead.");
        }

        Transaction t = new Transaction();
        t.setBook(book);
        t.setUser(user);
        t.setIssueDate(LocalDate.now());
        t.setDueDate(LocalDate.now().plusDays(loanPeriodDays));
        t.setStatus(TransactionStatus.ISSUED);
        transactionRepository.save(t);

        bookService.decrementAvailableCopies(book);
        notificationService.notify(user, "You have issued \"" + book.getTitle() + "\". Due back on " + t.getDueDate() + ".");

        return t;
    }

    /** Returns a book, calculates any overdue fine, and fulfills the next reservation if one exists. */
    public Transaction returnBook(Long transactionId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        t.setReturnDate(LocalDate.now());
        t.setStatus(TransactionStatus.RETURNED);

        long overdueDays = ChronoUnit.DAYS.between(t.getDueDate(), LocalDate.now());
        if (overdueDays > 0) {
            t.setFineAmount(overdueDays * finePerDay);
            notificationService.notify(t.getUser(),
                    "\"" + t.getBook().getTitle() + "\" was returned " + overdueDays + " day(s) late. Fine: ₹" + t.getFineAmount());
        }

        transactionRepository.save(t);
        bookService.incrementAvailableCopies(t.getBook());

        // if someone is waiting for this book, notify the next person in the reservation queue
        reservationService.fulfillNextReservation(t.getBook());

        return t;
    }

    public List<Transaction> findByUser(User user) {
        return transactionRepository.findByUserOrderByIssueDateDesc(user);
    }

    public List<Transaction> findAllIssued() {
        return transactionRepository.findByStatus(TransactionStatus.ISSUED);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAllByOrderByIssueDateDesc();
    }

    public List<Transaction> findOverdue() {
        return transactionRepository.findByStatusAndDueDateBefore(TransactionStatus.ISSUED, LocalDate.now());
    }

    public boolean hasActiveLoan(Long bookId, User user) {
        return transactionRepository.findByBookIdAndStatus(bookId, TransactionStatus.ISSUED)
                .stream().anyMatch(t -> t.getUser().getId().equals(user.getId()));
    }

    public long countIssued() {
        return transactionRepository.countByStatus(TransactionStatus.ISSUED);
    }

    public long countReturned() {
        return transactionRepository.countByStatus(TransactionStatus.RETURNED);
    }

    public double totalFinesCollected() {
        return transactionRepository.findByStatus(TransactionStatus.RETURNED).stream()
                .filter(Transaction::isFinePaid)
                .mapToDouble(Transaction::getFineAmount)
                .sum();
    }

    public double totalFinesOutstanding() {
        return transactionRepository.findByStatus(TransactionStatus.RETURNED).stream()
                .filter(t -> !t.isFinePaid())
                .mapToDouble(Transaction::getFineAmount)
                .sum();
    }

    public void markFinePaid(Long transactionId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        t.setFinePaid(true);
        transactionRepository.save(t);
    }
}
