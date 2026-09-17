package com.college.library.repository;

import com.college.library.entity.Transaction;
import com.college.library.entity.TransactionStatus;
import com.college.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByIssueDateDesc(User user);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByStatusAndDueDateBefore(TransactionStatus status, LocalDate date);
    List<Transaction> findByBookIdAndStatus(Long bookId, TransactionStatus status);
    List<Transaction> findAllByOrderByIssueDateDesc();
    long countByStatus(TransactionStatus status);
}
