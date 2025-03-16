package com.sporty.bookstore.dtos;

import java.math.BigDecimal;

public record BookPriceItem(Long bookId, BigDecimal price, double discount) {
}
