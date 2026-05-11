package com.veya.backend.notifications.dto;

import com.veya.backend.common.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        UUID userId,
        UUID familyId,
        NotificationType type,
        String title,
        String body,
        boolean read,
        Instant createdAt) {
}
