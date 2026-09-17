package com.college.library.service;

import com.college.library.entity.Book;
import com.college.library.entity.Transaction;
import com.college.library.repository.BookRepository;
import com.college.library.repository.TransactionRepository;
import com.college.library.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public ReportService(BookRepository bookRepository, UserRepository userRepository,
                          TransactionRepository transactionRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public long totalBooks() {
        return bookRepository.count();
    }

    public long totalCopies() {
        return bookRepository.findAll().stream().mapToInt(Book::getTotalCopies).sum();
    }

    public long totalUsers() {
        return userRepository.count();
    }

    public long currentlyIssuedCount() {
        return transactionRepository.findByStatus(com.college.library.entity.TransactionStatus.ISSUED).size();
    }

    public long overdueCount() {
        return transactionRepository.findByStatusAndDueDateBefore(
                com.college.library.entity.TransactionStatus.ISSUED, java.time.LocalDate.now()).size();
    }

    /** Top borrowed books, most issued first. */
    public List<Map.Entry<Book, Long>> mostBorrowedBooks(int limit) {
        List<Transaction> all = transactionRepository.findAll();

        // group by book ID (not the entity itself) since JPA doesn't guarantee
        // reference equality across separately-loaded proxies
        Map<Long, Long> countsById = all.stream()
                .collect(Collectors.groupingBy(t -> t.getBook().getId(), Collectors.counting()));
        Map<Long, Book> booksById = all.stream()
                .map(Transaction::getBook)
                .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));

        return countsById.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> Map.entry(booksById.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<Transaction> overdueList() {
        return transactionRepository.findByStatusAndDueDateBefore(
                com.college.library.entity.TransactionStatus.ISSUED, java.time.LocalDate.now());
    }
}
