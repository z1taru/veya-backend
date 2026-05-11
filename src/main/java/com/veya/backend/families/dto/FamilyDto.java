package com.veya.backend.families.dto;

import java.time.Instant;
import java.util.UUID;

public record FamilyDto(UUID id, String name, UUID ownerId, Instant createdAt) {
}
