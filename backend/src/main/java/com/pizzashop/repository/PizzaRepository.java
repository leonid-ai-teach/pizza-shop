package com.pizzashop.repository;

import com.pizzashop.entity.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PizzaRepository extends JpaRepository<Pizza, Long> {

    List<Pizza> findByActiveTrueOrderBySortOrderAsc();

    List<Pizza> findAllByOrderBySortOrderAsc();
}
