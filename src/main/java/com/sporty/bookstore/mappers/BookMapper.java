package com.sporty.bookstore.mappers;

import com.sporty.bookstore.dtos.BookData;
import com.sporty.bookstore.dtos.BookUpdate;
import com.sporty.bookstore.entities.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import static org.mapstruct.ReportingPolicy.IGNORE;

@Mapper(componentModel = "spring", unmappedTargetPolicy = IGNORE, unmappedSourcePolicy = IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookMapper {

    Book toEntity(BookData bookData);

    @Mapping(target = "id", ignore = true)
    void updateBookFromDto(BookUpdate bookUpdate, @MappingTarget Book book);
}
