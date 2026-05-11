package com.veya.backend.families.dto;

import com.veya.backend.common.enums.MemberRole;
import com.veya.backend.common.enums.MemberStatus;
import java.time.Instant;
import java.util.UUID;

public record FamilyMemberDto(
                UUID id,
                UUID userId,
                String fullName,
                String email,
                String avatarUrl,
                MemberRole role,
                MemberStatus status,
                Instant joinedAt) {
}