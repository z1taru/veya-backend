package com.veya.backend.families.dto;

import com.veya.backend.common.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull MemberRole role) {
}