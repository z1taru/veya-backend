package com.veya.backend.users.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 120) String fullName,
        @Size(max = 512) String avatarUrl) {
}