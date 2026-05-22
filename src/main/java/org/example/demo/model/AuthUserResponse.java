package org.example.demo.model;

public record AuthUserResponse(
        Long id,
        String username,
        String nickname
) {
}
