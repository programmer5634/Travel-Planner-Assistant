package org.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.demo.model.AuthResponse;
import org.example.demo.model.LoginRequest;
import org.example.demo.model.RegisterRequest;
import org.example.demo.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return authService.login(request, httpServletRequest);
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest httpServletRequest) {
        authService.logout(httpServletRequest);
        return new AuthResponse(false, null);
    }

    @GetMapping("/me")
    public AuthResponse currentUser(HttpServletRequest httpServletRequest) {
        return authService.currentUser(httpServletRequest);
    }
}
