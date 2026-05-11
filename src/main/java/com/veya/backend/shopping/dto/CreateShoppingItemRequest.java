package com.veya.backend.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShoppingItemRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String quantity,
        @Size(max = 100) String category) {
}
