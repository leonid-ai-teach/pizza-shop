package com.pizzashop.service;

import com.pizzashop.dto.AdminPizzaResponse;
import com.pizzashop.dto.PizzaWriteRequest;
import com.pizzashop.entity.Pizza;
import com.pizzashop.entity.Topping;
import com.pizzashop.exception.ResourceNotFoundException;
import com.pizzashop.exception.ValidationException;
import com.pizzashop.mapper.AdminCatalogMapper;
import com.pizzashop.repository.PizzaRepository;
import com.pizzashop.repository.ToppingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class PizzaAdminService {

    private final PizzaRepository pizzaRepository;
    private final ToppingRepository toppingRepository;

    public PizzaAdminService(PizzaRepository pizzaRepository, ToppingRepository toppingRepository) {
        this.pizzaRepository = pizzaRepository;
        this.toppingRepository = toppingRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminPizzaResponse> getPizzas() {
        return pizzaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(AdminCatalogMapper::toResponse)
                .toList();
    }

    public AdminPizzaResponse createPizza(PizzaWriteRequest request) {
        Pizza pizza = new Pizza(request.name(), request.description(), request.price(),
                request.imagePath(), true, request.sortOrder());
        pizza.replaceToppings(resolveToppings(request.toppingIds()));
        return AdminCatalogMapper.toResponse(pizzaRepository.save(pizza));
    }

    public AdminPizzaResponse updatePizza(Long id, PizzaWriteRequest request) {
        Pizza pizza = findPizza(id);
        pizza.updateDetails(request.name(), request.description(), request.price(),
                request.imagePath(), request.sortOrder());
        pizza.replaceToppings(resolveToppings(request.toppingIds()));
        return AdminCatalogMapper.toResponse(pizzaRepository.save(pizza));
    }

    public AdminPizzaResponse setActive(Long id, boolean active) {
        Pizza pizza = findPizza(id);
        pizza.setActive(active);
        return AdminCatalogMapper.toResponse(pizzaRepository.save(pizza));
    }

    /** Resolves the requested assignment wholesale, so an unknown id fails the whole update. */
    private Set<Topping> resolveToppings(List<Long> toppingIds) {
        Set<Long> distinctIds = new LinkedHashSet<>(toppingIds);
        Set<Topping> toppings = new LinkedHashSet<>(toppingRepository.findAllById(distinctIds));
        if (toppings.size() != distinctIds.size()) {
            throw new ValidationException("One or more toppings do not exist.");
        }
        return toppings;
    }

    private Pizza findPizza(Long id) {
        return pizzaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza not found: " + id));
    }
}
