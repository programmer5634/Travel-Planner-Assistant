package org.example.demo.model;

public record AuthResponse(
        boolean authenticated,
        AuthUserResponse user
) {
}
