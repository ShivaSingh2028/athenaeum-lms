package com.college.library.repository;

import com.college.library.entity.Transaction;
import com.college.library.entity.TransactionStatus;
import com.college.library.entity.User;
import com.college.library.tenant.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTenantIdOrderByIssueDateDesc(String tenantId);

    List<Transaction> findByTenantIdAndUserOrderByIssueDateDesc(String tenantId, User user);

    List<Transaction> findByTenantIdAndStatus(String tenantId, TransactionStatus status);

    List<Transaction> findByTenantIdAndStatusAndDueDateBefore(String tenantId, TransactionStatus status, LocalDate date);

    List<Transaction> findByTenantIdAndBookIdAndStatus(String tenantId, Long bookId, TransactionStatus status);

    long countByTenantIdAndStatus(String tenantId, TransactionStatus status);

    // Backward-compatibility delegating methods
    default List<Transaction> findByUserOrderByIssueDateDesc(User user) {
        return findByTenantIdAndUserOrderByIssueDateDesc(TenantContext.getTenantId(), user);
    }

    default List<Transaction> findByStatus(TransactionStatus status) {
        return findByTenantIdAndStatus(TenantContext.getTenantId(), status);
    }

    default List<Transaction> findAllByOrderByIssueDateDesc() {
        return findByTenantIdOrderByIssueDateDesc(TenantContext.getTenantId());
    }

    default List<Transaction> findByStatusAndDueDateBefore(TransactionStatus status, LocalDate date) {
        return findByTenantIdAndStatusAndDueDateBefore(TenantContext.getTenantId(), status, date);
    }

    default List<Transaction> findByBookIdAndStatus(Long bookId, TransactionStatus status) {
        return findByTenantIdAndBookIdAndStatus(TenantContext.getTenantId(), bookId, status);
    }

    default long countByStatus(TransactionStatus status) {
        return countByTenantIdAndStatus(TenantContext.getTenantId(), status);
    }
}
