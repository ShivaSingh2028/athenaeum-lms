package com.college.library.privacy;

import com.college.library.audit.AuditLogService;
import com.college.library.entity.Transaction;
import com.college.library.entity.User;
import com.college.library.finance.Fine;
import com.college.library.finance.FineRepository;
import com.college.library.payment.Payment;
import com.college.library.payment.PaymentRepository;
import com.college.library.repository.TransactionRepository;
import com.college.library.repository.UserRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PrivacyService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FineRepository fineRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public PrivacyService(UserRepository userRepository,
                          TransactionRepository transactionRepository,
                          FineRepository fineRepository,
                          PaymentRepository paymentRepository,
                          AuditLogService auditLogService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    public PatronDataExportDto exportPatronPersonalData(Long userId) {
        return exportPatronPersonalData(userId, null);
    }

    public PatronDataExportDto exportPatronPersonalData(Long userId, User currentUser) {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getTenantId() != null && !user.getTenantId().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Patron belongs to another library tenant.");
        }
        if (currentUser != null && !currentUser.getRole().isStaffOrAdmin() && !user.getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only export your own personal data.");
        }

        PatronDataExportDto dto = new PatronDataExportDto();
        dto.setPatronId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setMemberCode(user.getMemberCode());
        dto.setReadingHistoryPrivate(user.isReadingHistoryPrivate());

        // Export loan history
        List<Transaction> transactions = transactionRepository.findByTenantIdAndUserOrderByIssueDateDesc(tenantId, user);
        dto.setLoans(transactions.stream().map(t -> new PatronDataExportDto.LoanRecordDto(
                t.getBook() != null ? t.getBook().getTitle() : (t.getBookCopy() != null && t.getBookCopy().getBook() != null ? t.getBookCopy().getBook().getTitle() : "Book"),
                t.getBookCopy() != null ? t.getBookCopy().getBarcode() : "N/A",
                t.getIssueDate() != null ? t.getIssueDate().toString() : "",
                t.getDueDate() != null ? t.getDueDate().toString() : "",
                t.getReturnDate() != null ? t.getReturnDate().toString() : "ACTIVE",
                t.getStatus().name()
        )).collect(Collectors.toList()));

        // Export fines
        List<Fine> fines = fineRepository.findByTenantIdAndUserOrderByCreatedAtDesc(tenantId, user);
        dto.setFines(fines.stream().map(f -> new PatronDataExportDto.FineRecordDto(
                f.getAmount().toString(),
                f.getPaidAmount().toString(),
                f.getStatus().name(),
                f.getWaiverReason()
        )).collect(Collectors.toList()));

        // Export payments
        List<Payment> payments = paymentRepository.findByTenantIdAndUserOrderByPaymentDateDesc(tenantId, user);
        dto.setPayments(payments.stream().map(p -> new PatronDataExportDto.PaymentRecordDto(
                p.getAmount().toString(),
                p.getPaymentMethod().name(),
                p.getPaymentStatus().name(),
                p.getTransactionReference(),
                p.getPaymentDate() != null ? p.getPaymentDate().toString() : ""
        )).collect(Collectors.toList()));

        auditLogService.logAction(user.getId(), user.getEmail(), "EXPORT_PERSONAL_DATA", "User", user.getId().toString(), null, "SUCCESS");

        return dto;
    }

    @Transactional
    public void anonymizePatronAccount(Long userId) {
        anonymizePatronAccount(userId, null);
    }

    @Transactional
    public void anonymizePatronAccount(Long userId, User currentUser) {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getTenantId() != null && !user.getTenantId().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Patron belongs to another library tenant.");
        }
        if (currentUser != null && !currentUser.getRole().isStaffOrAdmin() && !user.getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only anonymize your own account.");
        }

        String oldEmail = user.getEmail();
        String oldName = user.getName();

        // Anonymize personal identifiable information
        user.setName("Anonymized Patron #" + user.getId());
        user.setEmail("anonymized-" + user.getId() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@privacy.local");
        user.setPhone(null);
        user.setMemberCode(null);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(false);

        userRepository.save(user);

        auditLogService.logAction(
                user.getId(), oldEmail, "ANONYMIZE_ACCOUNT_DPDP", "User", user.getId().toString(),
                "name=" + oldName + ", email=" + oldEmail, "ANONYMIZED"
        );
    }

    @Transactional
    public void setReadingHistoryPrivacy(Long userId, boolean isPrivate) {
        setReadingHistoryPrivacy(userId, isPrivate, null);
    }

    @Transactional
    public void setReadingHistoryPrivacy(Long userId, boolean isPrivate, User currentUser) {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getTenantId() != null && !user.getTenantId().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Patron belongs to another library tenant.");
        }
        if (currentUser != null && !currentUser.getRole().isStaffOrAdmin() && !user.getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only update your own privacy settings.");
        }

        user.setReadingHistoryPrivate(isPrivate);
        userRepository.save(user);
    }
}
