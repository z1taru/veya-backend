package com.veya.backend.tasks.dto;

import com.veya.backend.common.enums.RepeatType;
import com.veya.backend.common.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        UUID assigneeId,
        UUID familyId,
        TaskPriority priority,
        LocalDate dueDate,
        LocalTime dueTime,
        RepeatType repeatType) {
}