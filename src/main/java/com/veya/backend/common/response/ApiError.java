package com.veya.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<FieldError> errors;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}