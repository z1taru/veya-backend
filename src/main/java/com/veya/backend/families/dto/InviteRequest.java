package com.veya.backend.families.dto;

import com.veya.backend.common.enums.FamilyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteRequest(
        @NotBlank @Email String email,
        @NotNull FamilyRole role) {
}
