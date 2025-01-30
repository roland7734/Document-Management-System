package com.example.doc_management_syst.controllers;

import com.example.doc_management_syst.services.RegistrationService;
import com.example.doc_management_syst.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/")
    public String login(
        @RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "logout", required = false) String logout,
        Model model
    ) {
            if (error != null) {
                model.addAttribute("error", "Invalid username or password. Please try again.");
            }
            if (logout != null) {
                model.addAttribute("message", "You have been logged out successfully.");
            }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "registration";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            Model model
    ) {

        if (registrationService.existsByUsername(username)) {
            model.addAttribute("error", "Username already exists");
            return "registration";
        }
        if (registrationService.existsByEmail(email)) {
            model.addAttribute("error", "Email already exists");
            return "registration";
        }

        registrationService.registerUser(username, email, password);

        return "redirect:/";
    }
}
