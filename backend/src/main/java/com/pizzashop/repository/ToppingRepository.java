package com.pizzashop.repository;

import com.pizzashop.entity.Topping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToppingRepository extends JpaRepository<Topping, Long> {

    List<Topping> findByActiveTrueOrderByNameAsc();
}
