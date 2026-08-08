package com.pizzashop.controller;

import com.pizzashop.dto.PizzaResponse;
import com.pizzashop.service.PizzaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @GetMapping
    public List<PizzaResponse> getPizzas() {
        return pizzaService.getActivePizzas();
    }

    @GetMapping("/{id}")
    public PizzaResponse getPizza(@PathVariable Long id) {
        return pizzaService.getActivePizzaById(id);
    }
}
