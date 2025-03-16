package com.sporty.bookstore.dtos;

import com.sporty.bookstore.models.BookType;
import com.sporty.bookstore.validation.IsEnum;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;

public record BookUpdate (
        @Nullable
        @Size(min = 2, max = 50, message = "Book title must be between 2 and 50 characters long")
        String title,

        @Nullable
        @Positive(message = "Book price must be positive")
        @Max(value = 1000, message = "Book price must not exceed 1000")
        @Digits(integer = 4, fraction = 2, message = "Book price must have at most 2 decimal places")
        BigDecimal price,

        @Nullable
        @IsEnum(enumClass = BookType.class, message = "Book type must be valid enum value")
        String type
) {
}
