package com.veya.backend.common.exception;

// Convenience alias — delegates to ResourceNotFoundException
public class NotFoundException extends ResourceNotFoundException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, Object id) {
        super(resource, id);
    }
}