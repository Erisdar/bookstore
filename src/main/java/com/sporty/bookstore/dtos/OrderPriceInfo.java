package com.sporty.bookstore.dtos;

import java.math.BigDecimal;
import java.util.List;

public record OrderPriceInfo(Long userId, BigDecimal totalPrice, List<BookPriceItem> items) {
}
