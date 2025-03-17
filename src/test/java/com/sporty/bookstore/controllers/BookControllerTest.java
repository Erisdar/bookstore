package com.sporty.bookstore.controllers;

import com.sporty.bookstore.dtos.BookData;
import com.sporty.bookstore.dtos.BookUpdate;
import com.sporty.bookstore.entities.Book;
import com.sporty.bookstore.exceptions.ErrorResponse;
import com.sporty.bookstore.exceptions.FieldError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BookControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private R2dbcEntityTemplate postgresTemplate;

    private static final BookData TEST_BOOK = new BookData("Java", BigDecimal.valueOf(10.55), "NEW_RELEASES");

    @AfterEach
    void cleanUpDatabase() {
        postgresTemplate.getDatabaseClient()
            .sql("TRUNCATE TABLE books RESTART IDENTITY CASCADE")
            .fetch()
            .rowsUpdated()
            .block();
    }

    private static final List<Arguments> validBooks = List.of(
        arguments("JS", named("1000(Max price)", BigDecimal.valueOf(1000, 2)), "NEW_RELEASES"),
        arguments("Java", named("0.01(Min price)", BigDecimal.valueOf(0.01)), "OLD_EDITIONS"),
        arguments(named("(length=50)", "T".repeat(50)), BigDecimal.valueOf(50.55), "REGULAR")
    );

    @ParameterizedTest
    @FieldSource("validBooks")
    void test_create_book__success(String title, BigDecimal price, String type) {
        Instant beforeCreate = Instant.now();
        Book createdBook = this.createBook(new BookData(title, price, type));
        Instant afterCreate = Instant.now();

        webTestClient.get().uri("/books/%s".formatted(createdBook.getId()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Book.class)
            .value(book -> {
                assertThat(book.getId()).isPositive();
                assertThat(book.getTitle()).isEqualTo(title);
                assertThat(book.getPrice()).isEqualTo(price);
                assertThat(book.getType().name()).isEqualTo(type);
                assertThat(book.getCreatedAt()).isBetween(beforeCreate, afterCreate);
                assertThat(book.getUpdatedAt()).isEqualTo(book.getCreatedAt());
                assertThat(book).isEqualTo(createdBook);
            });
    }

    @Test
    void test_create_book_missing_body__bad_request() {
        webTestClient.post().uri("/books")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).contains("Invalid or missing request body");
                assertThat(errorResponse.errors()).isEmpty();
            });
    }

    private static final List<Arguments> nullTitleScenarios = List.of(
        arguments(null, "Book title can not be null")
    );

    private static final List<Arguments> invalidTitleScenarios = List.of(
        arguments("a", "Book title must be between 2 and 50 characters long"),
        arguments(named("a(length=51)", "a".repeat(51)), "Book title must be between 2 and 50 characters long")
    );

    @ParameterizedTest
    @FieldSource("nullTitleScenarios")
    @FieldSource("invalidTitleScenarios")
    void test_create_book_invalid_title__validation_errors(String title, String error) {
        webTestClient.post().uri("/books")
            .bodyValue(new BookData(title, TEST_BOOK.price(), TEST_BOOK.type()))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("title", error));
            });
    }

    private static final List<Arguments> nullPriceScenarios = List.of(
        arguments(null, "Book price can not be null")
    );

    private static final List<Arguments> invalidPriceScenarios = List.of(
        arguments(BigDecimal.valueOf(-1.00), "Book price must be positive"),
        arguments(BigDecimal.valueOf(0.00), "Book price must be positive"),
        arguments(BigDecimal.valueOf(1001.00), "Book price must not exceed 1000"),
        arguments(BigDecimal.valueOf(100.123), "Book price must have at most 2 decimal places")
    );

    @ParameterizedTest
    @FieldSource("nullPriceScenarios")
    @FieldSource("invalidPriceScenarios")
    void test_create_book_invalid_price__validation_errors(BigDecimal price, String error) {
        webTestClient.post().uri("/books")
            .bodyValue(new BookData(TEST_BOOK.title(), price, TEST_BOOK.type()))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("price", error));
            });
    }

    private static final List<Arguments> nullTypeScenarios = List.of(
        arguments(null, "Book type can not be null")
    );

    private static final List<Arguments> invalidTypeScenarios = List.of(
        arguments("INVALID_TYPE", "Book type must be valid enum value")
    );

    @ParameterizedTest
    @FieldSource("nullTypeScenarios")
    @FieldSource("invalidTypeScenarios")
    void test_create_book_invalid_type__validation_errors(String type, String error) {
        webTestClient.post().uri("/books")
            .bodyValue(new BookData(TEST_BOOK.title(), TEST_BOOK.price(), type))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("type", error));
            });
    }

    @ParameterizedTest
    @FieldSource("validBooks")
    void test_update_book__success(String title, BigDecimal price, String type) {
        BookUpdate bookUpdate = new BookUpdate(title, price, type);
        Book createdBook = this.createBook(TEST_BOOK);

        Instant beforeUpdate = Instant.now();
        Book updatedBook = webTestClient.patch().uri("/books/%s".formatted(createdBook.getId()))
            .bodyValue(bookUpdate)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Book.class)
            .value(book -> assertThat(book).isNotNull())
            .returnResult()
            .getResponseBody();

        Instant afterUpdate = Instant.now();

        webTestClient.get().uri("/books/%s".formatted(createdBook.getId()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Book.class)
            .value(book -> {
                assertThat(book.getTitle()).isEqualTo(bookUpdate.title());
                assertThat(book.getPrice()).isEqualTo(bookUpdate.price());
                assertThat(book.getType().name()).isEqualTo(bookUpdate.type());
                assertThat(book.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
                assertThat(book.getCreatedAt()).isEqualTo(createdBook.getCreatedAt());
                assertThat(book).isEqualTo(updatedBook);
            });
    }

    @Test
    void test_update_book_partial__success() {
        BookUpdate bookUpdate = new BookUpdate(null, null, null);
        Book createdBook = this.createBook(TEST_BOOK);

        Instant beforeUpdate = Instant.now();
        Book updatedBook = webTestClient.patch().uri("/books/%s".formatted(createdBook.getId()))
            .bodyValue(bookUpdate)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Book.class)
            .value(book -> assertThat(book).isNotNull())
            .returnResult()
            .getResponseBody();

        Instant afterUpdate = Instant.now();

        webTestClient.get().uri("/books/%s".formatted(createdBook.getId()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Book.class)
            .value(book -> {
                assertThat(book.getTitle()).isEqualTo(createdBook.getTitle());
                assertThat(book.getPrice()).isEqualTo(createdBook.getPrice());
                assertThat(book.getType()).isEqualTo(createdBook.getType());
                assertThat(book.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
                assertThat(book.getCreatedAt()).isEqualTo(createdBook.getCreatedAt());
                assertThat(book).isEqualTo(updatedBook);
            });
    }

    @Test
    void test_update_book__not_found_error() {
        Long nonExistentId = 100L;
        BookUpdate bookUpdate = new BookUpdate(TEST_BOOK.title(), null, null);

        webTestClient.patch().uri("/books/%s".formatted(nonExistentId))
            .bodyValue(bookUpdate)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Book with id %d is not found".formatted(nonExistentId));
                assertThat(errorResponse.errors()).isEmpty();
            });
    }

    @ParameterizedTest
    @FieldSource("invalidTitleScenarios")
    void test_update_book_invalid_title__validation_errors(String title, String error) {
        Book createdBook = this.createBook(TEST_BOOK);

        webTestClient.patch().uri("/books/%s".formatted(createdBook.getId()))
            .bodyValue(new BookUpdate(title, null, null))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("title", error));
            });
    }

    @ParameterizedTest
    @FieldSource("invalidPriceScenarios")
    void test_update_book_invalid_price__validation_errors(BigDecimal price, String error) {
        Book createdBook = this.createBook(TEST_BOOK);

        webTestClient.patch().uri("/books/%s".formatted(createdBook.getId()))
            .bodyValue(new BookUpdate(null, price, null))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("price", error));
            });
    }

    @ParameterizedTest
    @FieldSource("invalidTypeScenarios")
    void test_update_book_invalid_type__validation_errors(String type, String error) {
        Book createdBook = this.createBook(TEST_BOOK);

        webTestClient.patch().uri("/books/%s".formatted(createdBook.getId()))
            .bodyValue(new BookUpdate(null, null, type))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("type", error));
            });
    }

    @Test
    void test_get_books__success() {
        Book createdBook = this.createBook(TEST_BOOK);

        webTestClient.get().uri("/books")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Book.class)
            .hasSize(1)
            .value(books -> assertThat(books.getFirst()).isEqualTo(createdBook));
    }

    @Test
    void test_get_books__empty_list() {
        webTestClient.get().uri("/books")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Book.class)
            .value(books -> assertThat(books).isEmpty());
    }

    @Test
    void test_get_book__success() {
        Book createdBook = this.createBook(TEST_BOOK);

        webTestClient.get().uri("/books/%s".formatted(createdBook.getId()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Book.class)
            .value(book -> assertThat(book).isEqualTo(createdBook));
    }

    @Test
    void test_get_book__not_found_error() {
        Long nonExistentId = 10L;

        webTestClient.get().uri("/books/%s".formatted(nonExistentId))
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Book with id %d is not found".formatted(nonExistentId));
                assertThat(errorResponse.errors()).isEmpty();
            });
    }

    @Test
    void test_delete_book_success() {
        Book createdBook = this.createBook(TEST_BOOK);

        webTestClient.delete().uri("/books/%s".formatted(createdBook.getId()))
            .exchange()
            .expectStatus().isNoContent();

        webTestClient.get().uri("/books/%s".formatted(createdBook.getId()))
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Book with id %d is not found".formatted(createdBook.getId()));
                assertThat(errorResponse.errors()).isEmpty();
            });
    }

    private Book createBook(BookData bookData) {
        return webTestClient.post().uri("/books")
            .bodyValue(bookData)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(Book.class)
            .value(book -> assertThat(book).isNotNull())
            .returnResult()
            .getResponseBody();
    }
}
