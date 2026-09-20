package com.expensetrack.dto.auth;

public record AuthResponse(String token, String tokenType, String email, String name) {
}
