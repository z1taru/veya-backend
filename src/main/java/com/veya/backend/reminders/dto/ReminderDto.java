package com.veya.backend.reminders.dto;

import com.veya.backend.common.enums.RepeatType;
import com.veya.backend.common.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReminderDto(
        UUID id,
        UUID familyId,
        String title,
        String description,
        UUID creatorId,
        UUID assigneeId,
        LocalDate reminderDate,
        LocalTime reminderTime,
        RepeatType repeatType,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
