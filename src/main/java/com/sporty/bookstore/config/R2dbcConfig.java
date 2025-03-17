package com.sporty.bookstore.config;

import com.sporty.bookstore.entities.converters.BookTypeWriteConverter;
import com.sporty.bookstore.models.BookType;
import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.spi.Option;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories
@EnableTransactionManagement
public class R2dbcConfig {

    @Bean
    public ConnectionFactoryOptionsBuilderCustomizer enumCodecCustomizer() {
        return builder -> builder.option(Option.valueOf("extensions"), List.of(
            EnumCodec.builder().withEnum("book_type", BookType.class).build()
        ));
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, List.of(new BookTypeWriteConverter()));
    }
}
