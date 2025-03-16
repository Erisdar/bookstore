package com.sporty.bookstore.controllers;

import com.sporty.bookstore.dtos.AddBalance;
import com.sporty.bookstore.dtos.UserData;
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
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private R2dbcEntityTemplate postgresTemplate;

    private static final UserData TEST_USER = new UserData("Test", BigDecimal.valueOf(1000, 2));

    @AfterEach
    void cleanUpDatabase() {
        postgresTemplate.getDatabaseClient()
                .sql("TRUNCATE TABLE users RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();
    }

    private static final List<Arguments> validUsers = List.of(
            arguments("Y", named("1000(Max balance)", BigDecimal.valueOf(1000, 2))),
            arguments(named("(length=20)", "N".repeat(20)), named("0.01(Min balance)", BigDecimal.valueOf(0.01)))
    );

    @ParameterizedTest
    @FieldSource("validUsers")
    void test_create_user__success(String name, BigDecimal balance) {
        Instant beforeCreate = Instant.now();

        User createdUser = this.createUser(new UserData(name, balance));

        Instant afterCreate = Instant.now();

        webTestClient.get().uri("/users/%s".formatted(createdUser.getId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertThat(user.getId()).isPositive();
                    assertThat(user.getName()).isEqualTo(name);
                    assertThat(user.getBalance()).isEqualTo(balance);
                    assertThat(user.getLoyalty()).isZero();
                    assertThat(user.getCreatedAt()).isBetween(beforeCreate, afterCreate);
                    assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
                    assertThat(user).isEqualTo(createdUser);
                });
    }

    @Test
    void test_create_user_missing_body__bad_request() {
        webTestClient.post().uri("/users")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.message()).contains("Invalid or missing request body");
                    assertThat(errorResponse.errors()).isEmpty();
                });
    }

    private static final List<Arguments> invalidNameScenarios = List.of(
            arguments(null, "User name can not be null"),
            arguments("", "User name can not be empty"),
            arguments(named("(length=21)", "T".repeat(21)), "User name can not be longer than 20 symbols")
    );

    @ParameterizedTest
    @FieldSource("invalidNameScenarios")
    void test_create_user_invalid_name__validation_errors(String name, String error) {
        webTestClient.post().uri("/users")
                .bodyValue(new UserData(name, BigDecimal.valueOf(100.00)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                    assertThat(errorResponse.errors()).contains(new FieldError("name", error));
                });
    }

    private static final List<Arguments> invalidBalanceScenarios = List.of(
            arguments(BigDecimal.valueOf(-1.00), "User balance must be positive"),
            arguments(BigDecimal.valueOf(0.00), "User balance must be positive"),
            arguments(BigDecimal.valueOf(1001.00), "User balance must not exceed 1000"),
            arguments(BigDecimal.valueOf(100.123), "User balance must have at most 2 decimal places")
    );

    @ParameterizedTest
    @FieldSource("invalidBalanceScenarios")
    void test_create_user_invalid_balance__validation_errors(BigDecimal balance, String error) {
        webTestClient.post().uri("/users")
                .bodyValue(new UserData("Test", balance))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.message()).isEqualTo("Validation failed for one or more fields");
                    assertThat(errorResponse.errors()).contains(new FieldError("balance", error));
                });
    }

    @Test
    void test_add_balance__success() {
        AddBalance addBalance = new AddBalance(BigDecimal.valueOf(1000.00));
        User createdUser = this.createUser(TEST_USER);
        BigDecimal expectedBalance = createdUser.getBalance().add(addBalance.amount());

        Instant beforeUpdate = Instant.now();
        User updatedUser = webTestClient.patch().uri("/users/%s/balance/add".formatted(createdUser.getId()))
                .bodyValue(addBalance)
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        Instant afterUpdate = Instant.now();

        assertThat(updatedUser).isNotNull();

        webTestClient.get().uri("/users/%s".formatted(createdUser.getId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertThat(user.getBalance()).isEqualTo(expectedBalance);
                    assertThat(user.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
                    assertThat(user.getCreatedAt()).isEqualTo(createdUser.getCreatedAt());
                    assertThat(user.getName()).isEqualTo(createdUser.getName());
                    assertThat(user.getLoyalty()).isEqualTo(createdUser.getLoyalty());
                    assertThat(user).isEqualTo(updatedUser);
                });
    }

    @Test
    void test_add_balance__user_not_found() {
        Long nonExistentId = 10L;
        AddBalance addBalance = new AddBalance(BigDecimal.valueOf(50.00));

        webTestClient.patch().uri("/users/%s/balance/add".formatted(nonExistentId))
                .bodyValue(addBalance)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.message()).isEqualTo("User with id %d is not found".formatted(nonExistentId));
                    assertThat(errorResponse.errors()).isEmpty();
                });
    }

    private static final List<Arguments> invalidAmountScenarios = List.of(
            arguments(BigDecimal.valueOf(-1.00), "Amount must be positive"),
            arguments(BigDecimal.valueOf(0.00), "Amount must be positive"),
            arguments(BigDecimal.valueOf(1001.00), "Amount must not exceed 1000"),
            arguments(BigDecimal.valueOf(100.123), "Amount must have at most 2 decimal places")
    );

    @ParameterizedTest
    @FieldSource("invalidAmountScenarios")
    void test_add_balance_invalid_amount__validation_errors(BigDecimal amount, String error) {
        User createdUser = this.createUser(TEST_USER);

        webTestClient.patch().uri("/users/%s/balance/add".formatted(createdUser.getId()))
                .bodyValue(new AddBalance(amount))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> assertThat(errorResponse).isEqualTo(new ErrorResponse(
                        "Validation failed for one or more fields",
                        List.of(new FieldError("amount", error))
                )));
    }

    @Test
    void test_get_users__success() {
        User createdUser = this.createUser(TEST_USER);

        webTestClient.get().uri("/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .hasSize(1)
                .value(users -> assertThat(users.getFirst()).isEqualTo(createdUser));
    }

    @Test
    void test_get_users__empty_list() {
        webTestClient.get().uri("/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .hasSize(0);
    }

    @Test
    void test_get_user__success() {
        User createdUser = this.createUser(TEST_USER);

        webTestClient.get().uri("/users/%s".formatted(createdUser.getId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> assertThat(user).isEqualTo(createdUser));
    }

    @Test
    void test_get_user__not_found_error() {
        Long nonExistentId = 10L;

        webTestClient.get().uri("/users/%s".formatted(nonExistentId))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.message()).isEqualTo("User with id %d is not found".formatted(nonExistentId));
                    assertThat(errorResponse.errors()).isEmpty();
                });
    }

    @Test
    void test_delete_user__success() {
        User createdUser = this.createUser(TEST_USER);

        webTestClient.delete().uri("/users/%s".formatted(createdUser.getId()))
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get().uri("/users/%s".formatted(createdUser.getId()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertThat(errorResponse.message()).isEqualTo("User with id %d is not found".formatted(createdUser.getId()));
                    assertThat(errorResponse.errors()).isEmpty();
                });
    }

    private User createUser(UserData userData) {
        return webTestClient.post().uri("/users")
                .bodyValue(userData)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .value(user -> assertThat(user).isNotNull())
                .returnResult()
                .getResponseBody();
    }
}