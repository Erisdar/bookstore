package com.sporty.bookstore.controllers;

import com.sporty.bookstore.dtos.BookData;
import com.sporty.bookstore.dtos.BookUpdate;
import com.sporty.bookstore.entities.Book;
import com.sporty.bookstore.exceptions.types.BookNotFoundException;
import com.sporty.bookstore.mappers.BookMapper;
import com.sporty.bookstore.repositories.BookRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/books")
@AllArgsConstructor
public class BookController {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @GetMapping
    public Flux<Book> getBooks() {
        return bookRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<Book> getBook(@PathVariable Long id) {
        return bookRepository.findById(id)
            .switchIfEmpty(Mono.error(new BookNotFoundException(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Book> createBook(@Valid @RequestBody Mono<BookData> bookData) {
        return bookData
            .map(bookMapper::toEntity)
            .flatMap(bookRepository::save);
    }

    @PatchMapping("/{id}")
    public Mono<Book> updateBook(@PathVariable Long id, @Valid @RequestBody Mono<BookUpdate> bookUpdate) {
        return bookRepository.findById(id)
            .switchIfEmpty(Mono.error(new BookNotFoundException(id)))
            .flatMap(book -> bookUpdate.map(update -> {
                bookMapper.updateBookFromDto(update, book);
                return book;
            }))
            .flatMap(bookRepository::save);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteBook(@PathVariable Long id) {
        return bookRepository.findById(id)
            .switchIfEmpty(Mono.error(new BookNotFoundException(id)))
            .flatMap(book -> bookRepository.deleteById(book.getId()));
    }
}
