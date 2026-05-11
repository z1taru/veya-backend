package com.veya.backend.families.dto;

import com.veya.backend.common.enums.InviteStatus;
import com.veya.backend.common.enums.MemberRole;
import java.time.Instant;
import java.util.UUID;

public record InviteDto(
        UUID id,
        UUID familyId,
        String familyName,
        String email,
        MemberRole role,
        InviteStatus status,
        Instant expiresAt,
        Instant createdAt) {
}