package com.veya.backend.auth.dto;

import com.veya.backend.users.dto.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user) {
}