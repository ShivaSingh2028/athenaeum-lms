package com.college.library.policy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final MembershipTypeRepository membershipTypeRepository;

    public PolicyController(PolicyService policyService, MembershipTypeRepository membershipTypeRepository) {
        this.policyService = policyService;
        this.membershipTypeRepository = membershipTypeRepository;
    }

    @GetMapping
    public String listPolicies(Model model) {
        model.addAttribute("policies", policyService.getAllPolicies());
        return "admin-policies";
    }

    @PostMapping("/{id}")
    public String updatePolicy(@PathVariable Long id,
                               @RequestParam String displayName,
                               @RequestParam int maxBooksAllowed,
                               @RequestParam int loanDurationDays,
                               @RequestParam int renewalLimit,
                               @RequestParam BigDecimal fineRatePerDay,
                               @RequestParam int gracePeriodDays,
                               @RequestParam BigDecimal fineCapAmount,
                               @RequestParam(defaultValue = "false") boolean isActive) {
        MembershipType policy = membershipTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + id));

        policy.setDisplayName(displayName);
        policy.setMaxBooksAllowed(maxBooksAllowed);
        policy.setLoanDurationDays(loanDurationDays);
        policy.setRenewalLimit(renewalLimit);
        policy.setFineRatePerDay(fineRatePerDay);
        policy.setGracePeriodDays(gracePeriodDays);
        policy.setFineCapAmount(fineCapAmount);
        policy.setActive(isActive);

        policyService.updatePolicy(policy);
        return "redirect:/admin/policies?success=true";
    }
}
