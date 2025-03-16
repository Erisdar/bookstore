package com.sporty.bookstore.exceptions.types;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Long id) {
        super("User with id %d is not found".formatted(id));
    }
}
