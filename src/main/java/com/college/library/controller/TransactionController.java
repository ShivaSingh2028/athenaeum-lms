package com.college.library.controller;

import com.college.library.entity.Book;
import com.college.library.entity.User;
import com.college.library.service.BookService;
import com.college.library.service.TransactionService;
import com.college.library.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.college.library.entity.Transaction;

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
        model.addAttribute("books", bookService.findAll());
        return "transactions";
    }
    /** A student's own full transaction history, separate from the dashboard's compact view. */
    @GetMapping("/my")
    public String myTransactions(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("transactions", transactionService.findByUser(user));
        return "my-transactions";
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
    /** Students return their own borrowed book from their dashboard. */
    @PostMapping("/{id}/self-return")
    public String selfReturn(@PathVariable Long id, Model model) {
        Transaction t = transactionService.findById(id);
        User currentUser = userService.getCurrentUser();
        if (!t.getUser().getId().equals(currentUser.getId())) {
            model.addAttribute("error", "You can only return books issued to your own account.");
            return "redirect:/dashboard";
        }
        transactionService.returnBook(id);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/pay-fine")
    public String payFine(@PathVariable Long id) {
        transactionService.markFinePaid(id);
        return "redirect:/transactions";
    }
}
