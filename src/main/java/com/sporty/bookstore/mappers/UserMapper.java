package com.sporty.bookstore.mappers;

import com.sporty.bookstore.dtos.UserData;
import com.sporty.bookstore.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.ReportingPolicy.IGNORE;

@Mapper(componentModel = "spring", unmappedTargetPolicy = IGNORE, unmappedSourcePolicy = IGNORE)
public interface UserMapper {

    @Mapping(target = "loyalty", constant = "0")
    @Mapping(target = "balance", defaultValue = "0.00")
    User toEntity(UserData userData);
}
