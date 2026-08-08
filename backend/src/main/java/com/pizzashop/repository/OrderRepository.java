package com.pizzashop.repository;

import com.pizzashop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    Long nextOrderNumber();
}
