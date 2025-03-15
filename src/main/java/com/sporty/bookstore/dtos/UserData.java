package com.sporty.bookstore.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UserData (
    @NotNull(message = "User name can not be null")
    @NotBlank(message = "User name can not be empty")
    @Size(max = 20, message = "User name can not be longer than 20 symbols")
    String name,

    @Positive(message = "User balance must be positive")
    @Max(value = 1000, message = "User balance must not exceed 1000")
    @Digits(integer = 4, fraction = 2, message = "User balance must have at most 2 decimal places")
    BigDecimal balance
) {
}
