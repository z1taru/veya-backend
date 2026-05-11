package com.veya.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiParseRequest(@NotBlank String text) {
}
