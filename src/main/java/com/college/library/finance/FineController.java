package com.college.library.finance;

import com.college.library.entity.User;
import com.college.library.payment.Payment;
import com.college.library.payment.PaymentMethod;
import com.college.library.payment.PdfReceiptGenerator;
import com.college.library.payment.PaymentRepository;
import com.college.library.service.UserService;
import com.college.library.tenant.TenantContext;
import com.college.library.tenant.TenantService;
import com.college.library.tenant.TenantSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/finance")
public class FineController {

    private final FineService fineService;
    private final UserService userService;
    private final PaymentRepository paymentRepository;
    private final PdfReceiptGenerator receiptGenerator;
    private final TenantService tenantService;

    public FineController(FineService fineService,
                          UserService userService,
                          PaymentRepository paymentRepository,
                          PdfReceiptGenerator receiptGenerator,
                          TenantService tenantService) {
        this.fineService = fineService;
        this.userService = userService;
        this.paymentRepository = paymentRepository;
        this.receiptGenerator = receiptGenerator;
        this.tenantService = tenantService;
    }

    @GetMapping("/fines")
    public String allFines(Model model) {
        model.addAttribute("fines", fineService.getAllFines());
        model.addAttribute("totalCollected", fineService.getTotalFinesCollected());
        model.addAttribute("totalOutstanding", fineService.getTotalFinesOutstanding());
        return "finance-fines";
    }

    @GetMapping("/my-fines")
    public String myFines(Model model) {
        User user = userService.getCurrentUser();
        List<Fine> fines = fineService.getFinesForUser(user);
        BigDecimal unpaid = fineService.getUserUnpaidFineTotal(user);

        model.addAttribute("fines", fines);
        model.addAttribute("unpaidTotal", unpaid);
        model.addAttribute("user", user);
        return "finance-my-fines";
    }

    @PostMapping("/fines/{id}/waive")
    public String waiveFine(@PathVariable Long id,
                            @RequestParam(required = false) BigDecimal amount,
                            @RequestParam String reason,
                            RedirectAttributes redirectAttributes) {
        try {
            User staff = userService.getCurrentUser();
            fineService.waiveFine(id, amount, reason, staff);
            redirectAttributes.addFlashAttribute("successMessage", "Fine waived successfully with audit entry.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/finance/fines";
    }

    @PostMapping("/fines/{id}/pay-cash")
    public String payCash(@PathVariable Long id,
                          @RequestParam BigDecimal amount,
                          @RequestParam(required = false) String receiptNote,
                          RedirectAttributes redirectAttributes) {
        try {
            User staff = userService.getCurrentUser();
            Payment payment = fineService.recordPayment(id, amount, PaymentMethod.CASH, receiptNote, null, staff);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Cash payment of ₹" + amount + " recorded. Receipt ID: " + payment.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/finance/fines";
    }

    @GetMapping("/payments/{id}/receipt.pdf")
    @ResponseBody
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found: " + id));

        TenantSettings settings = tenantService.getSettings(TenantContext.getTenantId());
        try {
            byte[] pdf = receiptGenerator.generateReceiptPdf(payment, settings);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Receipt_" + payment.getId() + ".pdf")
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
