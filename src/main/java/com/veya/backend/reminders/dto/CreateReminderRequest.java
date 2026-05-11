package com.veya.backend.reminders.dto;

import com.veya.backend.common.enums.RepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateReminderRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        UUID assigneeId,
        LocalDate reminderDate,
        LocalTime reminderTime,
        RepeatType repeatType) {
}
