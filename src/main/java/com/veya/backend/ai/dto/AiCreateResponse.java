package com.veya.backend.ai.dto;

import com.veya.backend.common.enums.AiParsedType;
import com.veya.backend.common.enums.AiCommandStatus;

import java.util.List;
import java.util.UUID;

public record AiCreateResponse(
        UUID commandId,
        AiParsedType createdType,
        List<UUID> createdIds,
        AiCommandStatus status) {
}
