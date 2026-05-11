package com.veya.backend.users.dto;

import com.veya.backend.common.enums.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String fullName,
        String email,
        String avatarUrl,
        UserStatus status,
        Instant createdAt) {
}