package com.sporty.bookstore.dtos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AddBalance(
    @NotNull(message = "Amount can not be null")
    @Positive(message = "Amount must be positive")
    @Max(value = 1000, message = "Amount must not exceed 1000")
    @Digits(integer = 4, fraction = 2, message = "Amount must have at most 2 decimal places")
    BigDecimal amount
) {
}
