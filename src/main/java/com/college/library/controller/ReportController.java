package com.college.library.controller;

import com.college.library.service.ReportService;
import com.college.library.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final TransactionService transactionService;

    public ReportController(ReportService reportService, TransactionService transactionService) {
        this.reportService = reportService;
        this.transactionService = transactionService;
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
        model.addAttribute("finesCollected", transactionService.totalFinesCollected());
        model.addAttribute("finesOutstanding", transactionService.totalFinesOutstanding());
        return "reports";
    }
}
