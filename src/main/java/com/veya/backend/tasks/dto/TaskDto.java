package com.veya.backend.tasks.dto;

import com.veya.backend.common.enums.RepeatType;
import com.veya.backend.common.enums.TaskPriority;
import com.veya.backend.common.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TaskDto(
        UUID id,
        UUID familyId,
        String title,
        String description,
        UUID creatorId,
        UUID assigneeId,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        LocalTime dueTime,
        RepeatType repeatType,
        Instant createdAt,
        Instant updatedAt) {
}