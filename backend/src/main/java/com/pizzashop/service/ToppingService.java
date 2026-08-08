package com.pizzashop.service;

import com.pizzashop.dto.ToppingResponse;
import com.pizzashop.mapper.ToppingMapper;
import com.pizzashop.repository.ToppingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ToppingService {

    private final ToppingRepository toppingRepository;

    public ToppingService(ToppingRepository toppingRepository) {
        this.toppingRepository = toppingRepository;
    }

    public List<ToppingResponse> getActiveToppings() {
        return toppingRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(ToppingMapper::toResponse)
                .toList();
    }
}
