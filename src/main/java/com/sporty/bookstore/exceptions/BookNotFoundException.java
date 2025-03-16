package com.sporty.bookstore.exceptions;

public class BookNotFoundException extends NotFoundException {
    public BookNotFoundException(Long id) {
        super("Book with id %d is not found".formatted(id));
    }
}
