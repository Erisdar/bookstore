package com.sporty.bookstore.services;

import com.sporty.bookstore.dtos.BookItem;
import com.sporty.bookstore.dtos.BookPriceItem;
import com.sporty.bookstore.dtos.OrderDetails;
import com.sporty.bookstore.dtos.OrderPriceInfo;
import com.sporty.bookstore.entities.Book;
import com.sporty.bookstore.entities.Order;
import com.sporty.bookstore.entities.OrderItem;
import com.sporty.bookstore.exceptions.types.BookNotFoundException;
import com.sporty.bookstore.exceptions.types.InsufficientBalanceException;
import com.sporty.bookstore.exceptions.types.LoyaltyNotAcceptableException;
import com.sporty.bookstore.exceptions.types.UserNotFoundException;
import com.sporty.bookstore.models.BookType;
import com.sporty.bookstore.repositories.BookRepository;
import com.sporty.bookstore.repositories.OrderItemRepository;
import com.sporty.bookstore.repositories.OrderRepository;
import com.sporty.bookstore.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final int BUNDLE_SIZE = 3;
    private static final BigDecimal REGULAR_BUNDLE_DISCOUNT = BigDecimal.valueOf(0.9);
    private static final BigDecimal OLD_EDITION_DISCOUNT = BigDecimal.valueOf(0.8);
    private static final BigDecimal OLD_EDITION_BUNDLE_DISCOUNT = BigDecimal.valueOf(0.75);

    @Transactional
    public Mono<Order> createOrder(Mono<OrderDetails> orderDetails) {
        return this.getOrderPriceInfo(orderDetails)
            .flatMap(this::deductUserBalance)
            .flatMap(this::saveOrderWithItems);
    }

    public Mono<OrderPriceInfo> getOrderPriceInfo(Mono<OrderDetails> orderDetailsMono) {
        return orderDetailsMono
            .flatMap(this::validateUserLoyaltyOrError)
            .flatMap(orderDetails -> {
                var bookQuantitityMap = orderDetails.items().stream()
                    .collect(Collectors.toMap(BookItem::bookId, BookItem::quantity, Integer::sum));

                return bookRepository.findAllById(bookQuantitityMap.keySet())
                    .collectMap(Book::getId)
                    .flatMap(booksMap -> validateOrderBooksOrError(booksMap, orderDetails))
                    .map(booksMap -> this.duplicateBooks(booksMap, bookQuantitityMap))
                    .map(books -> this.applyLoyalty(orderDetails, books))
                    .map(books -> calculateBookPrices(orderDetails, books, bookQuantitityMap));
            });
    }

    private List<Book> duplicateBooks(Map<Long, Book> booksMap, Map<Long, Integer> bookQuantitityMap) {
        return booksMap.values().stream()
            .flatMap(book -> Stream.generate(book::copy).limit(bookQuantitityMap.get(book.getId())))
            .toList();
    }

    private List<Book> applyLoyalty(OrderDetails orderDetails, List<Book> books) {
        if (orderDetails.loyaltyBookId() != null) {
            books.stream()
                .filter(book -> orderDetails.loyaltyBookId().equals(book.getId()))
                .findFirst()
                .ifPresent(book -> book.setPrice(BigDecimal.ZERO));
        }
        return books;
    }

    private OrderPriceInfo calculateBookPrices(OrderDetails orderDetails, List<Book> books, Map<Long, Integer> bookQuantitityMap) {
        return books.stream()
            .map(book -> this.getBookPrice(book, bookQuantitityMap))
            .collect(Collectors.teeing(
                    Collectors.reducing(BigDecimal.ZERO, BookPriceItem::price, BigDecimal::add),
                    Collectors.toList(),
                    (total, items) -> new OrderPriceInfo(orderDetails.userId(), total, items)
                )
            );
    }

    private Mono<Map<Long, Book>> validateOrderBooksOrError(Map<Long, Book> booksMap, OrderDetails orderDetails) {
        var missingIds = orderDetails.items().stream()
            .map(BookItem::bookId)
            .filter(bookId -> !booksMap.containsKey(bookId))
            .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            return Mono.error(new BookNotFoundException(missingIds));
        }

        var loyaltyBookType = Optional.ofNullable(booksMap.get(orderDetails.loyaltyBookId()))
            .map(Book::getType)
            .orElse(null);

        if (BookType.NEW_RELEASES.equals(loyaltyBookType)) {
            return Mono.error(new LoyaltyNotAcceptableException(BookType.NEW_RELEASES));
        }
        return Mono.just(booksMap);
    }

    private Mono<OrderPriceInfo> deductUserBalance(OrderPriceInfo orderPriceInfo) {
        return userRepository.findById(orderPriceInfo.userId())
            .switchIfEmpty(Mono.error(new UserNotFoundException(orderPriceInfo.userId())))
            .flatMap(user -> {
                if (user.getBalance().compareTo(orderPriceInfo.totalPrice()) < 0) {
                    return Mono.error(new InsufficientBalanceException());
                }
                user.setBalance(user.getBalance().subtract(orderPriceInfo.totalPrice()));
                return userRepository.save(user)
                    .thenReturn(orderPriceInfo);
            });
    }

    private Mono<Order> saveOrderWithItems(OrderPriceInfo orderPriceInfo) {
        return orderRepository.save(new Order(orderPriceInfo.userId(), orderPriceInfo.totalPrice()))
            .flatMap(savedOrder -> {
                var orderItems = orderPriceInfo.items().stream()
                    .map(item -> new OrderItem(savedOrder.getId(), item.bookId(), item.price()))
                    .toList();

                return orderItemRepository.saveAll(orderItems)
                    .collectList()
                    .map(savedItems -> {
                        savedOrder.setOrderItems(savedItems);
                        return savedOrder;
                    });
            });
    }

    private Mono<OrderDetails> validateUserLoyaltyOrError(OrderDetails details) {
        return userRepository.findById(details.userId())
            .switchIfEmpty(Mono.error(new UserNotFoundException(details.userId())))
            .flatMap(user -> {
                if (details.loyaltyBookId() != null && user.getLoyalty() < 10) {
                    return Mono.error(new LoyaltyNotAcceptableException(user.getLoyalty()));
                }
                return Mono.just(details);
            });
    }


    private BookPriceItem getBookPrice(Book book, Map<Long, Integer> bookQuantitityMap) {
        if (book.getPrice().equals(BigDecimal.ZERO)) {
            return new BookPriceItem(book.getId(), book.getPrice(), 100);
        }

        int bundleSize = bookQuantitityMap.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
        boolean isBundle = bundleSize >= BUNDLE_SIZE;

        return switch (book.getType()) {
            case NEW_RELEASES -> new BookPriceItem(book.getId(), book.getPrice(), 0);
            case REGULAR -> isBundle
                ? new BookPriceItem(book.getId(), book.getPrice().multiply(REGULAR_BUNDLE_DISCOUNT), this.discountPercent(REGULAR_BUNDLE_DISCOUNT))
                : new BookPriceItem(book.getId(), book.getPrice(), 0);
            case OLD_EDITIONS -> {
                var discount = isBundle ? OLD_EDITION_BUNDLE_DISCOUNT : OLD_EDITION_DISCOUNT;
                yield new BookPriceItem(book.getId(), book.getPrice().multiply(discount), this.discountPercent(discount));
            }
        };
    }

    private double discountPercent(BigDecimal discountValue) {
        return BigDecimal.valueOf(1).subtract(discountValue).multiply(BigDecimal.valueOf(100)).doubleValue();
    }
}
