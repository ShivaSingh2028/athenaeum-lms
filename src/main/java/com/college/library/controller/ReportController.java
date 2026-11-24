package com.college.library.controller;

import com.college.library.audit.StockAuditEntry;
import com.college.library.audit.StockAuditSession;
import com.college.library.audit.StockAuditService;
import com.college.library.service.ReportService;
import com.college.library.service.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'SUPER_ADMIN', 'LIBRARY_ADMIN')")
public class ReportController {

    private final ReportService reportService;
    private final TransactionService transactionService;
    private final StockAuditService stockAuditService;

    public ReportController(ReportService reportService,
                            TransactionService transactionService,
                            StockAuditService stockAuditService) {
        this.reportService = reportService;
        this.transactionService = transactionService;
        this.stockAuditService = stockAuditService;
    }

    @GetMapping
    public String reports(Model model) {
        model.addAttribute("totalBooks", reportService.totalBooks());
        model.addAttribute("totalCopies", reportService.totalCopies());
        model.addAttribute("totalUsers", reportService.totalUsers());
        model.addAttribute("issuedCount", reportService.currentlyIssuedCount());
        model.addAttribute("overdueCount", reportService.overdueCount());
        model.addAttribute("mostBorrowed", reportService.mostBorrowedBooks(5));
        model.addAttribute("overdueList", reportService.overdueList());
        model.addAttribute("deadStock", reportService.deadStockBooks());
        model.addAttribute("agingBuckets", reportService.overdueAgingAnalysis());
        model.addAttribute("finesCollected", transactionService.totalFinesCollected());
        model.addAttribute("finesOutstanding", transactionService.totalFinesOutstanding());
        model.addAttribute("auditSessions", stockAuditService.listSessions());
        return "reports";
    }

    @GetMapping("/export-pdf")
    public void exportCirculationPdf(HttpServletResponse response) {
        try {
            byte[] pdfBytes = reportService.generateCirculationReportPdf();
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"athenaeum_circulation_report.pdf\"");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // --- Physical Stock Verification ---

    @PostMapping("/stock-audit/start")
    public String startAuditSession(@RequestParam String sessionName, RedirectAttributes redirectAttributes) {
        StockAuditSession session = stockAuditService.startSession(sessionName);
        redirectAttributes.addFlashAttribute("successMessage", "Stock audit session started: " + session.getSessionName());
        return "redirect:/reports/stock-audit/" + session.getId();
    }

    @GetMapping("/stock-audit/{id}")
    public String viewAuditSession(@PathVariable Long id, Model model) {
        StockAuditSession session = stockAuditService.getSession(id);
        List<StockAuditEntry> entries = stockAuditService.getSessionEntries(id);
        model.addAttribute("session", session);
        model.addAttribute("entries", entries);
        return "stock-audit";
    }

    @PostMapping("/stock-audit/{id}/scan")
    public String scanCopyForAudit(@PathVariable Long id,
                                   @RequestParam String barcode,
                                   @RequestParam String shelfLocation,
                                   RedirectAttributes redirectAttributes) {
        try {
            StockAuditEntry entry = stockAuditService.scanCopy(id, barcode, shelfLocation);
            redirectAttributes.addFlashAttribute("scanResult", "Scanned " + barcode + " -> " + entry.getStatus());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reports/stock-audit/" + id;
    }

    @PostMapping("/stock-audit/{id}/finalize")
    public String finalizeAuditSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        StockAuditSession session = stockAuditService.finalizeSession(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Audit session finalized! Scanned: " + session.getScannedCount() + ", Missing copies flagged: " + session.getMissingCount());
        return "redirect:/reports/stock-audit/" + id;
    }
}
