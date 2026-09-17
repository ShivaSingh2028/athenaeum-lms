package com.college.library.controller;

import com.college.library.entity.Book;
import com.college.library.entity.User;
import com.college.library.service.BookService;
import com.college.library.service.ReservationService;
import com.college.library.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final BookService bookService;
    private final UserService userService;

    public ReservationController(ReservationService reservationService, BookService bookService,
                                  UserService userService) {
        this.reservationService = reservationService;
        this.bookService = bookService;
        this.userService = userService;
    }

    @PostMapping("/reserve/{bookId}")
    public String reserve(@PathVariable Long bookId, Model model) {
        Book book = bookService.findById(bookId);
        User user = userService.getCurrentUser();
        try {
            reservationService.reserve(book, user);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "redirect:/books";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return "redirect:/dashboard";
    }
}
