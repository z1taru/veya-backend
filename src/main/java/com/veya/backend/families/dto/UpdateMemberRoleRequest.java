package com.veya.backend.families.dto;

import com.veya.backend.common.enums.FamilyRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull FamilyRole role) {
}
