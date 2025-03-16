package com.sporty.bookstore.exceptions.types;

import java.util.Set;

public class BookNotFoundException extends NotFoundException {
    public BookNotFoundException(Long id) {
        super("Book with id %d is not found".formatted(id));
    }

    public BookNotFoundException(Set<Long> ids) {
        super("Book with ids %s is not found".formatted(ids));
    }
}
