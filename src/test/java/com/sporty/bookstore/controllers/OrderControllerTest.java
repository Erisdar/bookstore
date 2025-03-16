package com.sporty.bookstore.controllers;


import com.sporty.bookstore.dtos.*;
import com.sporty.bookstore.entities.Book;
import com.sporty.bookstore.entities.User;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private R2dbcEntityTemplate postgresTemplate;

    private static final BookData NEW_RELEASE_BOOK = new BookData("JS", BigDecimal.valueOf(100, 2), "NEW_RELEASES");
    private static final BookData REGULAR_BOOK = new BookData("Java", BigDecimal.valueOf(50, 2), "REGULAR");
    private static final BookData OLD_EDITION_BOOK = new BookData("Kotlin", BigDecimal.valueOf(25, 2), "OLD_EDITIONS");

    private static final UserData TEST_USER = new UserData("Test", BigDecimal.valueOf(1000, 2));

    @AfterEach
    void cleanUpDatabase() {
        postgresTemplate.getDatabaseClient()
            .sql("TRUNCATE TABLE books, users RESTART IDENTITY CASCADE")
            .fetch()
            .rowsUpdated()
            .block();
    }

    private static final List<Arguments> invalidUserIdScenarios = List.of(
        arguments(null, "User id can not be null"),
        arguments(0L, "User id must be positive"),
        arguments(-1L, "User id must be positive")
    );

    @ParameterizedTest
    @FieldSource("invalidUserIdScenarios")
    void test_calculate_price_invalid_user_id__validation_errors(Long userId, String error) {
        OrderDetails orderDetails = new OrderDetails(userId, null, List.of(new BookItem(1L, 1)));

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("userId", error));
            });
    }

    private static final List<Arguments> invalidLoyaltyBookIdScenarios = List.of(
        arguments(0L, "LoyaltyBookId id must be positive"),
        arguments(-1L, "LoyaltyBookId id must be positive")
    );

    @ParameterizedTest
    @FieldSource("invalidLoyaltyBookIdScenarios")
    void test_calculate_price_invalid_loyalty_book_id__validation_errors(Long loyaltyBookId, String error) {
        OrderDetails orderDetails = new OrderDetails(1L, loyaltyBookId, List.of(new BookItem(1L, 1)));

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("loyaltyBookId", error));
            });
    }

    private static final List<Arguments> emptyItemsScenarios = List.of(
        arguments(List.of(), "Items can not be empty")
    );

    @ParameterizedTest
    @FieldSource("emptyItemsScenarios")
    void test_calculate_price_empty_items__validation_errors(List<BookItem> items, String error) {
        OrderDetails orderDetails = new OrderDetails(1L, null, items);

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError("items", error));
            });
    }

    private static final List<Arguments> invalidBookItemScenarios = List.of(
        arguments(null, 1, "items[0].bookId", "Book id can not be null"),
        arguments(0L, 1, "items[0].bookId", "Book id must be positive"),
        arguments(-1L, 1, "items[0].bookId", "Book id must be positive"),
        arguments(1L, null, "items[0].quantity", "Quantity can not be null"),
        arguments(1L, -1, "items[0].quantity", "Quantity must be positive"),
        arguments(1L, 0, "items[0].quantity", "Quantity must be positive"),
        arguments(1L, 11, "items[0].quantity", "Quantity must not exceed 10")
    );

    @ParameterizedTest
    @FieldSource("invalidBookItemScenarios")
    void test_calculate_price_invalid_book_item__validation_errors(Long bookId, Integer quantity, String field, String error) {
        BookItem invalidBookItem = new BookItem(bookId, quantity);
        OrderDetails orderDetails = new OrderDetails(1L, null, List.of(invalidBookItem));

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                assertThat(errorResponse.errors()).contains(new FieldError(field, error));
            });
    }

    @Test
    void test_calculate_price__book_not_found_error() {
        User user = createUser(TEST_USER);
        Long nonExistentBookId = 10L;

        OrderDetails orderDetails = new OrderDetails(user.getId(), null, List.of(new BookItem(nonExistentBookId, 1)));

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("Book with ids [%s] is not found".formatted(nonExistentBookId));
            });
    }

    @Test
    void test_calculate_price__user_not_found_error() {
        Book book = createBook(NEW_RELEASE_BOOK);
        Long nonExistentUserId = 10L;

        OrderDetails orderDetails = new OrderDetails(nonExistentUserId, null, List.of(new BookItem(book.getId(), 1)));

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("User with id %d is not found".formatted(nonExistentUserId));
            });
    }

    @Test
    void test_calculate_price__insufficient_loyalty_error() {
        User user = createUser(new UserData("Test User", BigDecimal.valueOf(20)));
        Book book = createBook(REGULAR_BOOK);

        OrderDetails orderDetails = new OrderDetails(user.getId(), book.getId(), List.of(new BookItem(book.getId(), 1)));

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse -> {
                assertThat(errorResponse.message()).isEqualTo("User has insufficient loyalty: 0");
            });
    }

    @Test
    void test_calculate_price__not_acceptable_book_type_loyalty_error() {
        User user = createUser(new UserData("Test User", BigDecimal.valueOf(20)));
        Book book = createBook(NEW_RELEASE_BOOK);
        addUserLoyalty(user.getId());

        OrderDetails orderDetails = new OrderDetails(user.getId(), book.getId(), List.of(new BookItem(book.getId(), 1)));

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ErrorResponse.class)
            .value(errorResponse ->
                assertThat(errorResponse.message())
                    .isEqualTo("Loyalty discount cannot be applied to books of type: NEW_RELEASES"));
    }

    @Test
    void test_calculate_price_bundle_order__success() {
        User user = createUser(new UserData("Test User", BigDecimal.valueOf(10)));

        Book newReleaseBook = createBook(NEW_RELEASE_BOOK);
        Book regularBook = createBook(REGULAR_BOOK);
        Book oldEditionBook = createBook(OLD_EDITION_BOOK);
        Book oldEditionBook2 = createBook(OLD_EDITION_BOOK);

        BigDecimal expectedRegularPrice = regularBook.getPrice().multiply(BigDecimal.valueOf(0.9));
        BigDecimal expectedOldEditionPrice = oldEditionBook.getPrice().multiply(BigDecimal.valueOf(0.75));
        BigDecimal totalPrice = BigDecimal.ZERO
            .add(expectedRegularPrice)
            .add(expectedRegularPrice)
            .add(expectedOldEditionPrice)
            .add(expectedOldEditionPrice)
            .add(expectedOldEditionPrice)
            .add(newReleaseBook.getPrice())
            .add(newReleaseBook.getPrice());


        OrderDetails orderDetails = new OrderDetails(
            user.getId(),
            null,
            List.of(
                new BookItem(newReleaseBook.getId(), 2),
                new BookItem(regularBook.getId(), 2),
                new BookItem(oldEditionBook.getId(), 1),
                new BookItem(oldEditionBook.getId(), 1),
                new BookItem(oldEditionBook2.getId(), 1)
            )
        );

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderPriceInfo.class)
            .value(priceInfo -> {
                assertThat(priceInfo.totalPrice()).isEqualByComparingTo(totalPrice);
                assertThat(priceInfo.items())
                    .hasSize(7)
                    .containsAll(List.of(
                        new BookPriceItem(newReleaseBook.getId(), newReleaseBook.getPrice(), 0),
                        new BookPriceItem(newReleaseBook.getId(), newReleaseBook.getPrice(), 0),
                        new BookPriceItem(regularBook.getId(), expectedRegularPrice, 10),
                        new BookPriceItem(regularBook.getId(), expectedRegularPrice, 10),
                        new BookPriceItem(oldEditionBook.getId(), expectedOldEditionPrice, 25),
                        new BookPriceItem(oldEditionBook.getId(), expectedOldEditionPrice, 25),
                        new BookPriceItem(oldEditionBook2.getId(), expectedOldEditionPrice, 25)
                    ));
            });
    }

    @Test
    void test_calculate_price_with_loyalty_discount_bundle__success() {
        User user = createUser(new UserData("Test User", BigDecimal.valueOf(10)));
        addUserLoyalty(user.getId());

        Book regularBook = createBook(REGULAR_BOOK);
        Book oldEditionBook = createBook(OLD_EDITION_BOOK);
        Book oldEditionBook2 = createBook(OLD_EDITION_BOOK);

        BigDecimal expectedRegularPrice = regularBook.getPrice().multiply(BigDecimal.valueOf(0.9));
        BigDecimal expectedOldEditionPrice = oldEditionBook.getPrice().multiply(BigDecimal.valueOf(0.75));
        BigDecimal totalPrice = BigDecimal.ZERO
            .add(expectedRegularPrice)
            .add(expectedOldEditionPrice)
            .add(expectedOldEditionPrice);

        OrderDetails orderDetails = new OrderDetails(
            user.getId(),
            regularBook.getId(),
            List.of(
                new BookItem(regularBook.getId(), 2),
                new BookItem(oldEditionBook.getId(), 1),
                new BookItem(oldEditionBook2.getId(), 1)
            )
        );

        webTestClient.post().uri("/orders/calculate-price")
            .bodyValue(orderDetails)
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderPriceInfo.class)
            .value(priceInfo -> {
                assertThat(priceInfo.totalPrice()).isEqualByComparingTo(totalPrice);
                assertThat(priceInfo.items())
                    .hasSize(4)
                    .containsAll(List.of(
                        new BookPriceItem(regularBook.getId(), BigDecimal.ZERO, 100),
                        new BookPriceItem(regularBook.getId(), expectedRegularPrice, 10),
                        new BookPriceItem(oldEditionBook.getId(), expectedOldEditionPrice, 25),
                        new BookPriceItem(oldEditionBook2.getId(), expectedOldEditionPrice, 25)
                    ));
            });
    }

    private void addUserLoyalty(Long userId) {
        postgresTemplate.getDatabaseClient()
            .sql("UPDATE USERS SET loyalty = 10 WHERE id = %d".formatted(userId))
            .fetch()
            .rowsUpdated()
            .block();
    }

    private Book createBook(BookData bookData) {
        return webTestClient.post().uri("/books")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(bookData)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(Book.class)
            .returnResult()
            .getResponseBody();
    }

    private User createUser(UserData userData) {
        return webTestClient.post().uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userData)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(User.class)
            .returnResult()
            .getResponseBody();
    }
}