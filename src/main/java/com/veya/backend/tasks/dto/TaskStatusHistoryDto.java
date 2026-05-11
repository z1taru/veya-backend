package com.veya.backend.tasks.dto;

import com.veya.backend.common.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskStatusHistoryDto(
        UUID id,
        UUID taskId,
        UUID changedById,
        TaskStatus oldStatus,
        TaskStatus newStatus,
        String comment,
        Instant createdAt) {
}