package com.sporty.bookstore.entities;

import com.sporty.bookstore.models.BookType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Table("books")
public class Book extends BaseEntity {

    @Column("title")
    private String title;

    @Column("price")
    private BigDecimal price;

    @Column("type")
    private BookType type;

    public Book copy() {
        Book copy = new Book();
        copy.setId(this.getId());
        copy.setTitle(this.title);
        copy.setPrice(this.price);
        copy.setType(this.type);
        return copy;
    }
}
