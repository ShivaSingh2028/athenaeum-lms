package com.college.library.controller;

import com.college.library.entity.*;
import com.college.library.policy.PolicyService;
import com.college.library.service.BookCopyService;
import com.college.library.service.TransactionService;
import com.college.library.service.UserService;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping({"/circulation/desk", "/circulation-desk"})
public class CirculationDeskController {

    private final TransactionService transactionService;
    private final BookCopyService bookCopyService;
    private final UserService userService;
    private final PolicyService policyService;

    public CirculationDeskController(TransactionService transactionService,
                                     BookCopyService bookCopyService,
                                     UserService userService,
                                     PolicyService policyService) {
        this.transactionService = transactionService;
        this.bookCopyService = bookCopyService;
        this.userService = userService;
        this.policyService = policyService;
    }

    @GetMapping
    public String deskView(Model model) {
        model.addAttribute("recentTransactions", transactionService.findAll().stream().limit(8).toList());
        return "circulation-desk";
    }

    @PostMapping("/scan-issue")
    public String scanIssue(@RequestParam String patronIdentifier,
                            @RequestParam String copyBarcode,
                            RedirectAttributes redirectAttributes) {
        try {
            // 1. Locate patron by barcode member code or email
            String trimmedPatron = patronIdentifier.trim();
            User patron = userService.findAll().stream()
                    .filter(u -> trimmedPatron.equalsIgnoreCase(u.getEmail()) || 
                                 trimmedPatron.equalsIgnoreCase(u.getMemberCode()) ||
                                 trimmedPatron.equalsIgnoreCase("LIB-" + u.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No registered patron found matching '" + trimmedPatron + "'"));

            // 2. Locate physical book copy by barcode
            BookCopy copy = bookCopyService.findByBarcode(copyBarcode)
                    .orElseThrow(() -> new IllegalArgumentException("No book copy registered with barcode '" + copyBarcode + "'"));

            // 3. Validate against Policy Engine
            long activeLoans = transactionService.findByUser(patron).stream()
                    .filter(t -> t.getStatus() == TransactionStatus.ISSUED)
                    .count();
            policyService.validateCanBorrow(patron, copy, activeLoans);

            // 4. Issue physical copy atomically
            User staff = userService.getCurrentUser();
            Transaction tx = transactionService.issueCopy(copy, patron, staff);

            redirectAttributes.addFlashAttribute("successMessage", 
                    "Successfully issued \"" + copy.getBook().getTitle() + "\" (" + copy.getBarcode() 
                    + ") to " + patron.getName() + ". Due date: " + tx.getDueDate());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/circulation/desk";
    }

    @PostMapping("/scan-return")
    public String scanReturn(@RequestParam String copyBarcode,
                             RedirectAttributes redirectAttributes) {
        try {
            // 1. Locate physical copy
            BookCopy copy = bookCopyService.findByBarcode(copyBarcode)
                    .orElseThrow(() -> new IllegalArgumentException("No book copy registered with barcode '" + copyBarcode + "'"));

            // 2. Locate active issued transaction
            Transaction activeTx = transactionService.findAllIssued().stream()
                    .filter(t -> t.getBookCopy() != null && t.getBookCopy().getId().equals(copy.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No active borrow transaction found for copy barcode '" + copyBarcode + "'"));

            // 3. Return book copy
            Transaction returnedTx = transactionService.returnBook(activeTx.getId());

            String msg = "Returned \"" + copy.getBook().getTitle() + "\" from " + returnedTx.getUser().getName() + ".";
            if (returnedTx.getFineAmount() > 0) {
                msg += " Overdue Fine Assessed: ₹" + returnedTx.getFineAmount();
            }
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/circulation/desk";
    }
}
