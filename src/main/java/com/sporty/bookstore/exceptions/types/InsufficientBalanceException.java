package com.sporty.bookstore.exceptions.types;

public class InsufficientBalanceException extends BadRequestException {
    public InsufficientBalanceException() {
        super("User has insufficient balance");
    }
}
