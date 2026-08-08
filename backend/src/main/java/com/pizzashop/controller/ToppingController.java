package com.pizzashop.controller;

import com.pizzashop.dto.ToppingResponse;
import com.pizzashop.service.ToppingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/toppings")
public class ToppingController {

    private final ToppingService toppingService;

    public ToppingController(ToppingService toppingService) {
        this.toppingService = toppingService;
    }

    @GetMapping
    public List<ToppingResponse> getToppings() {
        return toppingService.getActiveToppings();
    }
}
