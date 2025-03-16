package com.sporty.bookstore.entities;

import lombok.*;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@RequiredArgsConstructor
@Table("orders")
public class Order extends BaseEntity {

    @Column("user_id")
    @NonNull
    private Long userId;

    @Column("total_price")
    @NonNull
    private BigDecimal totalPrice;

    @Transient
    private List<OrderItem> orderItems;
}
