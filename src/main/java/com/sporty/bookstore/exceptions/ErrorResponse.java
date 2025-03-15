package com.sporty.bookstore.exceptions;

import java.util.Collections;
import java.util.List;

public record ErrorResponse(String message, List<FieldError> errors) {
    public ErrorResponse(String message) {
        this(message, Collections.emptyList());
    }
}
