package com.veya.backend.families.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFamilyRequest(@NotBlank @Size(min = 1, max = 120) String name) {
}
