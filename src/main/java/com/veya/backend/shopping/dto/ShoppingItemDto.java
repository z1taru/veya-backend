package com.veya.backend.shopping.dto;

import java.time.Instant;
import java.util.UUID;

public record ShoppingItemDto(
        UUID id,
        UUID familyId,
        String name,
        String quantity,
        String category,
        UUID addedById,
        boolean completed,
        UUID completedById,
        Instant createdAt,
        Instant updatedAt) {
}
