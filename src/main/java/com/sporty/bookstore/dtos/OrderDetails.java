package com.sporty.bookstore.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderDetails(
    @NotNull(message = "User id can not be null")
    @Positive(message = "User id must be positive")
    Long userId,

    @Positive(message = "LoyaltyBookId id must be positive")
    Long loyaltyBookId,

    @NotEmpty(message = "Items can not be empty")
    @Size(min = 1, message = "Items must contain at least 1 item")
    List<@Valid BookItem> items
) {
}
