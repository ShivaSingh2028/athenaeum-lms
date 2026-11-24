package com.college.library.controller;

import com.college.library.entity.Book;
import com.college.library.entity.User;
import com.college.library.service.BookService;
import com.college.library.service.TransactionService;
import com.college.library.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/my")
    public String myTransactions(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("transactions", transactionService.findByUser(user));
        return "my-transactions";
    }

    @PostMapping("/issue/{bookId}")
    public String issueSelf(@PathVariable Long bookId, RedirectAttributes redirectAttributes) {
        try {
            Book book = bookService.findById(bookId);
            User user = userService.getCurrentUser();
            transactionService.issueBook(book, user);
            redirectAttributes.addFlashAttribute("success", "Successfully borrowed \"" + book.getTitle() + "\"!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }

    @PostMapping("/{id}/renew")
    public String renew(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser();
            transactionService.renewLoan(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Loan renewed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transactions/my";
    }

    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            transactionService.returnBook(id);
            redirectAttributes.addFlashAttribute("success", "Book return recorded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/self-return")
    public String selfReturn(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser();
            transactionService.selfReturnBook(id, user);
            redirectAttributes.addFlashAttribute("success", "Book returned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/pay-fine")
    public String payFine(@PathVariable Long id) {
        transactionService.markFinePaid(id);
        return "redirect:/transactions";
    }
}
