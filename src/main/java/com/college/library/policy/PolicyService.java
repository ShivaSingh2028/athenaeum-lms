package com.college.library.policy;

import com.college.library.entity.BookCopy;
import com.college.library.entity.CopyStatus;
import com.college.library.entity.Transaction;
import com.college.library.entity.User;
import com.college.library.tenant.TenantContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PolicyService {

    private final MembershipTypeRepository membershipTypeRepository;

    public PolicyService(MembershipTypeRepository membershipTypeRepository) {
        this.membershipTypeRepository = membershipTypeRepository;
    }

    public List<MembershipType> getAllPolicies() {
        initDefaultPoliciesIfMissing(TenantContext.getTenantId());
        return membershipTypeRepository.findByTenantId(TenantContext.getTenantId());
    }

    @Cacheable(value = "policies", key = "#tenantId + '_' + #code")
    public MembershipType getPolicy(String tenantId, String code) {
        return membershipTypeRepository.findByTenantIdAndCode(tenantId, code)
                .orElseGet(() -> defaultPolicy(tenantId, code));
    }

    public MembershipType getPolicyForUser(User user) {
        String code = user.getRole().name();
        if (code.equals("MEMBER") || code.equals("STUDENT")) {
            code = "UG";
        }
        return getPolicy(user.getTenantId(), code);
    }

    public LocalDate calculateDueDate(User user) {
        MembershipType policy = getPolicyForUser(user);
        return LocalDate.now().plusDays(policy.getLoanDurationDays());
    }

    public BigDecimal calculateFine(Transaction transaction, LocalDate returnDate) {
        MembershipType policy = getPolicyForUser(transaction.getUser());
        LocalDate dueDate = transaction.getDueDate();

        if (!returnDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }

        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (daysLate <= policy.getGracePeriodDays()) {
            return BigDecimal.ZERO;
        }

        BigDecimal fine = policy.getFineRatePerDay().multiply(BigDecimal.valueOf(daysLate));
        if (policy.getFineCapAmount() != null && fine.compareTo(policy.getFineCapAmount()) > 0) {
            fine = policy.getFineCapAmount();
        }
        return fine;
    }

    public void validateCanBorrow(User user, BookCopy copy, long currentActiveLoans) {
        if (!user.isEnabled()) {
            throw new IllegalStateException("Patron account is disabled.");
        }
        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Book copy " + copy.getBarcode() + " is currently " + copy.getStatus());
        }
        MembershipType policy = getPolicyForUser(user);
        if (currentActiveLoans >= policy.getMaxBooksAllowed()) {
            throw new IllegalStateException("Borrowing quota exceeded. Maximum allowed for " 
                    + policy.getDisplayName() + " is " + policy.getMaxBooksAllowed() + " book(s).");
        }
    }

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public MembershipType updatePolicy(MembershipType policy) {
        return membershipTypeRepository.save(policy);
    }

    @Transactional
    public void initDefaultPoliciesIfMissing(String tenantId) {
        if (!membershipTypeRepository.existsByTenantIdAndCode(tenantId, "UG")) {
            membershipTypeRepository.save(new MembershipType("UG", "Undergraduate Student", 3, 14, 2, new BigDecimal("5.00")));
        }
        if (!membershipTypeRepository.existsByTenantIdAndCode(tenantId, "PG")) {
            membershipTypeRepository.save(new MembershipType("PG", "Postgraduate Student", 5, 21, 3, new BigDecimal("5.00")));
        }
        if (!membershipTypeRepository.existsByTenantIdAndCode(tenantId, "FACULTY")) {
            membershipTypeRepository.save(new MembershipType("FACULTY", "Faculty / Professor", 10, 60, 5, new BigDecimal("2.00")));
        }
        if (!membershipTypeRepository.existsByTenantIdAndCode(tenantId, "STAFF")) {
            membershipTypeRepository.save(new MembershipType("STAFF", "Administrative Staff", 4, 30, 2, new BigDecimal("3.00")));
        }
        if (!membershipTypeRepository.existsByTenantIdAndCode(tenantId, "GUEST")) {
            membershipTypeRepository.save(new MembershipType("GUEST", "Visiting Scholar / Guest", 2, 7, 1, new BigDecimal("10.00")));
        }
    }

    private MembershipType defaultPolicy(String tenantId, String code) {
        return new MembershipType(code, code, 3, 14, 2, new BigDecimal("5.00"));
    }
}
