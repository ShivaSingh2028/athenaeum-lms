package com.college.library.controller;

import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
public class AdminController {

    private final UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin-users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role) {
        User user = userService.findById(id);
        user.setRole(role);
        userService.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggleEnabled(@PathVariable Long id) {
        User user = userService.findById(id);
        user.setEnabled(!user.isEnabled());
        userService.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/add")
    public String addStaff(@RequestParam String name, @RequestParam String email,
                            @RequestParam String password, @RequestParam Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        userService.save(user);
        return "redirect:/admin/users";
    }
}
