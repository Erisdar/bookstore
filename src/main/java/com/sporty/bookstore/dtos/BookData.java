package com.sporty.bookstore.dtos;

import com.sporty.bookstore.models.BookType;
import com.sporty.bookstore.validation.IsEnum;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BookData(
        @NotNull(message = "Book title can not be null")
        @Size(min = 2, max = 50, message = "Book title must be between 2 and 50 characters long")
        String title,

        @NotNull(message = "Book price can not be null")
        @Positive(message = "Book price must be positive")
        @Max(value = 1000, message = "Book price must not exceed 1000")
        @Digits(integer = 4, fraction = 2, message = "Book price must have at most 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Book type can not be null")
        @IsEnum(enumClass = BookType.class, message = "Book type must be valid enum value")
        String type
) {
}
