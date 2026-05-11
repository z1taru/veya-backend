package com.veya.backend.tasks.dto;

import com.veya.backend.common.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull TaskStatus status,
        String comment) {
}