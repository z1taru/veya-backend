package com.veya.backend.ai.dto;

import com.veya.backend.common.enums.AiCommandStatus;
import com.veya.backend.common.enums.AiParsedType;

import java.util.UUID;

public record AiParseResponse(
        UUID commandId,
        AiParsedType parsedType,
        String parsedPayload,
        AiCommandStatus status) {
}
