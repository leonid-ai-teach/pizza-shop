package com.pizzashop.repository;

import com.pizzashop.entity.Order;
import com.pizzashop.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByPublicToken(String publicToken);

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    Long nextOrderNumber();

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);
}
