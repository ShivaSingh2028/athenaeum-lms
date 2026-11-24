package com.college.library.finance;

import com.college.library.entity.Transaction;
import com.college.library.entity.User;
import com.college.library.payment.Payment;
import com.college.library.payment.PaymentMethod;
import com.college.library.payment.PaymentRepository;
import com.college.library.payment.PaymentStatus;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FineService {

    private final FineRepository fineRepository;
    private final PaymentRepository paymentRepository;

    public FineService(FineRepository fineRepository, PaymentRepository paymentRepository) {
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<Fine> getAllFines() {
        return fineRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    public List<Fine> getFinesForUser(User user) {
        return fineRepository.findByTenantIdAndUserOrderByCreatedAtDesc(TenantContext.getTenantId(), user);
    }

    public Fine getById(Long id) {
        return fineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fine record not found: " + id));
    }

    public BigDecimal getUserUnpaidFineTotal(User user) {
        return fineRepository.sumUnpaidFinesForUser(TenantContext.getTenantId(), user);
    }

    public BigDecimal getTotalFinesCollected() {
        return fineRepository.sumTotalPaidFines(TenantContext.getTenantId());
    }

    public BigDecimal getTotalFinesOutstanding() {
        return fineRepository.sumTotalOutstandingFines(TenantContext.getTenantId());
    }

    @Transactional
    public Fine accrueFine(Transaction transaction, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        Fine fine = new Fine(transaction, transaction.getUser(), amount);
        fine.setTenantId(transaction.getTenantId());
        return fineRepository.save(fine);
    }

    @Transactional
    public Fine waiveFine(Long fineId, BigDecimal amountToWaive, String reason, User staff) {
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("Mandatory audit reason must be provided to waive a library fine.");
        }
        Fine fine = getById(fineId);
        BigDecimal outstanding = fine.getOutstandingAmount();

        if (amountToWaive == null || amountToWaive.compareTo(BigDecimal.ZERO) <= 0) {
            amountToWaive = outstanding; // default to full waiver
        }
        if (amountToWaive.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("Waiver amount (₹" + amountToWaive + ") exceeds outstanding fine (₹" + outstanding + ")");
        }

        fine.setWaivedAmount(fine.getWaivedAmount().add(amountToWaive));
        fine.setWaivedBy(staff);
        fine.setWaiverReason(reason.trim());

        if (fine.getOutstandingAmount().compareTo(BigDecimal.ZERO) == 0) {
            fine.setStatus(FineStatus.WAIVED);
        } else {
            fine.setStatus(FineStatus.PARTIALLY_PAID);
        }

        return fineRepository.save(fine);
    }

    @Transactional
    public Payment recordPayment(Long fineId, BigDecimal amount, PaymentMethod method, 
                                  String reference, String idempotencyKey, User recordedBy) {
        Fine fine = fineId != null ? getById(fineId) : null;
        User patron = fine != null ? fine.getUser() : recordedBy;

        // Idempotency guard: prevent duplicate settlements
        if (idempotencyKey != null && paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey).get();
        }

        Payment payment = new Payment();
        payment.setTenantId(TenantContext.getTenantId());
        payment.setFine(fine);
        payment.setUser(patron);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(reference != null ? reference : "CASH-" + System.currentTimeMillis());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setRecordedBy(recordedBy);

        Payment savedPayment = paymentRepository.save(payment);

        if (fine != null) {
            fine.setPaidAmount(fine.getPaidAmount().add(amount));
            if (fine.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                fine.setStatus(FineStatus.PAID);
            } else {
                fine.setStatus(FineStatus.PARTIALLY_PAID);
            }
            fineRepository.save(fine);
        }

        return savedPayment;
    }
}
