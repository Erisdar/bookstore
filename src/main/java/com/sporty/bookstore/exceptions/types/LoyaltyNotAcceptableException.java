package com.sporty.bookstore.exceptions.types;

import com.sporty.bookstore.models.BookType;

public class LoyaltyNotAcceptableException extends BadRequestException {
    public LoyaltyNotAcceptableException(Integer loyalty) {
        super("User has insufficient loyalty: %d".formatted(loyalty));
    }

    public LoyaltyNotAcceptableException(BookType type) {
        super("Loyalty discount cannot be applied to books of type: %s".formatted(type.name()));
    }

    public LoyaltyNotAcceptableException(Long loyaltyBookId) {
        super("Loyalty book with id %s is not included in the order.".formatted(loyaltyBookId));
    }
}
