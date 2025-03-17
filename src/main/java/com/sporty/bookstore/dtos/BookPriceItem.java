package com.sporty.bookstore.dtos;

import java.math.BigDecimal;
import java.util.Objects;

public record BookPriceItem(Long bookId, BigDecimal price, double discount) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookPriceItem that = (BookPriceItem) o;
        return Double.compare(discount, that.discount) == 0 &&
            Objects.equals(bookId, that.bookId) &&
            (price == null ? that.price == null :
                price.compareTo(that.price) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            bookId,
            price != null ? price.stripTrailingZeros() : null,
            discount
        );
    }
}
