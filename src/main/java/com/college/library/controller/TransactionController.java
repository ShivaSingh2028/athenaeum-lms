package com.college.library.controller;

import com.college.library.entity.Book;
import com.college.library.entity.User;
import com.college.library.service.BookService;
import com.college.library.service.TransactionService;
import com.college.library.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final BookService bookService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, BookService bookService,
                                  UserService userService) {
        this.transactionService = transactionService;
        this.bookService = bookService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("transactions", transactionService.findAll());
        return "transactions";
    }

    /** Student issues a book to themselves. */
    @PostMapping("/issue/{bookId}")
    public String issueSelf(@PathVariable Long bookId, Model model) {
        Book book = bookService.findById(bookId);
        User user = userService.getCurrentUser();
        try {
            transactionService.issueBook(book, user);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }

    /** Librarian/Admin issues a book to a specific user (by email) at the front desk. */
    @PostMapping("/issue/{bookId}/to")
    public String issueToUser(@PathVariable Long bookId, @RequestParam String email, Model model) {
        Book book = bookService.findById(bookId);
        User user = userService.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No user with email " + email));
        transactionService.issueBook(book, user);
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Long id) {
        transactionService.returnBook(id);
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/pay-fine")
    public String payFine(@PathVariable Long id) {
        transactionService.markFinePaid(id);
        return "redirect:/transactions";
    }
}
