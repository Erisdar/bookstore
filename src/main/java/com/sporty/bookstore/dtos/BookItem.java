package com.sporty.bookstore.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookItem(
    @NotNull(message = "Book id can not be null")
    @Positive(message = "Book id must be positive")
    Long bookId,

    @NotNull(message = "Quantity can not be null")
    @Positive(message = "Quantity must be positive")
    @Max(value = 10, message = "Quantity must not exceed 10")
    Integer quantity
) {
}
