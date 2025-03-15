package com.sporty.bookstore.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Table("users")
public class User extends BaseEntity {

    private String name;

    @Column
    private BigDecimal balance;

    @Column
    private Integer loyalty;
}
