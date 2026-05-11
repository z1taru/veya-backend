package com.veya.backend.families.dto;

import com.veya.backend.common.enums.MemberRole;
import com.veya.backend.common.enums.MemberStatus;
import java.time.Instant;
import java.util.UUID;

public record FamilyDto(UUID id, String name, UUID ownerId, Instant createdAt) {
}