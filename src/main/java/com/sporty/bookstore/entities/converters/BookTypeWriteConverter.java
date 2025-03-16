package com.sporty.bookstore.entities.converters;

import com.sporty.bookstore.models.BookType;
import org.springframework.data.r2dbc.convert.EnumWriteSupport;

public class BookTypeWriteConverter extends EnumWriteSupport<BookType> {

}
