package com.sporty.bookstore.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("order_items")
public class OrderItem extends BaseEntity {

    @Column("order_id")
    private Long orderId;

    @Column("book_id")
    private Long bookId;

    @Column("price")
    private BigDecimal price;
}
