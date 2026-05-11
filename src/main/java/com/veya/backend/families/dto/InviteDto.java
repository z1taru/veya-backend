package com.veya.backend.families.dto;

import com.veya.backend.common.enums.FamilyRole;
import com.veya.backend.common.enums.InviteStatus;
import java.time.Instant;
import java.util.UUID;

public record InviteDto(
        UUID id,
        UUID familyId,
        String familyName,
        String email,
        FamilyRole role,
        InviteStatus status,
        Instant expiresAt,
        Instant createdAt) {
}
