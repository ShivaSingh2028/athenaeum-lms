package com.college.library.payment;

import com.college.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByTenantIdOrderByPaymentDateDesc(String tenantId);

    List<Payment> findByTenantIdAndUserOrderByPaymentDateDesc(String tenantId, User user);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByTransactionReference(String transactionReference);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
