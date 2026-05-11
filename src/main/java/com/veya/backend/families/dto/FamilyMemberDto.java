package com.veya.backend.families.dto;

import com.veya.backend.common.enums.FamilyMemberStatus;
import com.veya.backend.common.enums.FamilyRole;
import java.time.Instant;
import java.util.UUID;

public record FamilyMemberDto(
                UUID id,
                UUID userId,
                String fullName,
                String email,
                String avatarUrl,
                FamilyRole role,
                FamilyMemberStatus status,
                Instant joinedAt) {
}
