package com.sporty.bookstore.repositories;

import com.sporty.bookstore.entities.OrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {
}
