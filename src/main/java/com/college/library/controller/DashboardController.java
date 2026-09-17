package com.college.library.controller;

import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;
    private final ReportService reportService;

    public DashboardController(UserService userService, TransactionService transactionService,
                                ReservationService reservationService, NotificationService notificationService,
                                ReportService reportService) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("notifications", notificationService.forUser(user));
        model.addAttribute("unreadCount", notificationService.unreadCount(user));

        if (user.getRole() == Role.ADMIN || user.getRole() == Role.LIBRARIAN) {
            model.addAttribute("totalBooks", reportService.totalBooks());
            model.addAttribute("totalCopies", reportService.totalCopies());
            model.addAttribute("totalUsers", reportService.totalUsers());
            model.addAttribute("issuedCount", reportService.currentlyIssuedCount());
            model.addAttribute("overdueCount", reportService.overdueCount());
            model.addAttribute("pendingReservations", reservationService.findPending());
            return "dashboard-admin";
        } else {
            model.addAttribute("myLoans", transactionService.findByUser(user));
            model.addAttribute("myReservations", reservationService.findByUser(user));
            return "dashboard-student";
        }
    }
}
