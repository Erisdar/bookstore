package com.sporty.bookstore.controllers;

import com.sporty.bookstore.dtos.OrderDetails;
import com.sporty.bookstore.dtos.OrderPriceInfo;
import com.sporty.bookstore.entities.Order;
import com.sporty.bookstore.repositories.OrderRepository;
import com.sporty.bookstore.services.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping
    public Flux<Order> getOrders() {
        return orderService.findAll();
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> createOrder(@Valid @RequestBody Mono<OrderDetails> orderDetails) {
        return orderService.createOrder(orderDetails);
    }

    @PostMapping("/calculate-price")
    @ResponseStatus(HttpStatus.OK)
    public Mono<OrderPriceInfo> getOrderPriceInfo(@Valid @RequestBody Mono<OrderDetails> orderDetails) {
        return orderService.getOrderPriceInfo(orderDetails);
    }

}
