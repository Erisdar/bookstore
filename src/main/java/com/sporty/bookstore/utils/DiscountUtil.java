package com.sporty.bookstore.utils;

import com.sporty.bookstore.entities.Book;

import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_UP;

public class DiscountUtil {

    private static final int BUNDLE_SIZE = 3;
    private static final BigDecimal NO_DISCOUNT = BigDecimal.valueOf(0);
    private static final BigDecimal REGULAR_BUNDLE_DISCOUNT = BigDecimal.valueOf(10);
    private static final BigDecimal OLD_EDITION_DISCOUNT = BigDecimal.valueOf(20);
    private static final BigDecimal OLD_EDITION_BUNDLE_DISCOUNT = BigDecimal.valueOf(25);

    public static BigDecimal getDiscount(Book book, int totalBooks) {
        var isBundle = totalBooks >= BUNDLE_SIZE;
        return switch (book.getType()) {
            case NEW_RELEASES -> NO_DISCOUNT;
            case REGULAR -> isBundle
                ? REGULAR_BUNDLE_DISCOUNT
                : NO_DISCOUNT;
            case OLD_EDITIONS -> isBundle
                ? OLD_EDITION_BUNDLE_DISCOUNT
                : OLD_EDITION_DISCOUNT;
        };
    }

    public static BigDecimal applyDiscount(BigDecimal price, BigDecimal discountPercentage) {
        BigDecimal discountFactor = discountPercentage.divide(BigDecimal.valueOf(100), 2, HALF_UP);
        BigDecimal discountAmount = BigDecimal.valueOf(1).subtract(discountFactor);

        return price.multiply(discountAmount).setScale(2, HALF_UP);
    }
}
