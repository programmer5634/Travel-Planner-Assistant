package org.example.demo.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.demo.model.AuthResponse;
import org.example.demo.model.LoginRequest;
import org.example.demo.model.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, HttpServletRequest httpServletRequest);

    AuthResponse currentUser(HttpServletRequest httpServletRequest);

    void logout(HttpServletRequest httpServletRequest);
}
