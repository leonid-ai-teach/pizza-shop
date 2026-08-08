package com.pizzashop.service;

import com.pizzashop.dto.PizzaResponse;
import com.pizzashop.entity.Pizza;
import com.pizzashop.exception.ResourceNotFoundException;
import com.pizzashop.mapper.PizzaMapper;
import com.pizzashop.repository.PizzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PizzaService {

    private final PizzaRepository pizzaRepository;

    public PizzaService(PizzaRepository pizzaRepository) {
        this.pizzaRepository = pizzaRepository;
    }

    public List<PizzaResponse> getActivePizzas() {
        return pizzaRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(PizzaMapper::toResponse)
                .toList();
    }

    public PizzaResponse getActivePizzaById(Long id) {
        Pizza pizza = pizzaRepository.findById(id)
                .filter(Pizza::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza not found: " + id));
        return PizzaMapper.toResponse(pizza);
    }
}
