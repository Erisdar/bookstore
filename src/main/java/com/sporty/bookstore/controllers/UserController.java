package com.sporty.bookstore.controllers;

import com.sporty.bookstore.dtos.AddBalance;
import com.sporty.bookstore.dtos.UserData;
import com.sporty.bookstore.entities.User;
import com.sporty.bookstore.exceptions.UserNotFoundException;
import com.sporty.bookstore.mappers.UserMapper;
import com.sporty.bookstore.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping
    public Flux<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> createUser(@Valid @RequestBody Mono<UserData> userData) {
        return userData
                .map(userMapper::toEntity)
                .flatMap(userRepository::save);
    }

    @PatchMapping("/{id}/balance/add")
    public Mono<User> addBalance(@PathVariable Long id, @Valid @RequestBody Mono<AddBalance> newBalance) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(user -> newBalance.map(update -> {
                    user.setBalance(user.getBalance().add(update.amount()));
                    return user;
                }))
                .flatMap(userRepository::save);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(user -> userRepository.deleteById(user.getId()));
    }
}
