package com.pizzashop.service;

import com.pizzashop.dto.AdminToppingResponse;
import com.pizzashop.dto.ToppingWriteRequest;
import com.pizzashop.entity.Topping;
import com.pizzashop.exception.ResourceNotFoundException;
import com.pizzashop.mapper.AdminCatalogMapper;
import com.pizzashop.repository.ToppingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ToppingAdminService {

    private final ToppingRepository toppingRepository;

    public ToppingAdminService(ToppingRepository toppingRepository) {
        this.toppingRepository = toppingRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminToppingResponse> getToppings() {
        return toppingRepository.findAllByOrderByNameAsc().stream()
                .map(AdminCatalogMapper::toResponse)
                .toList();
    }

    public AdminToppingResponse createTopping(ToppingWriteRequest request) {
        Topping topping = new Topping(request.name(), request.description(), request.price(), true);
        return AdminCatalogMapper.toResponse(toppingRepository.save(topping));
    }

    public AdminToppingResponse updateTopping(Long id, ToppingWriteRequest request) {
        Topping topping = findTopping(id);
        topping.updateDetails(request.name(), request.description(), request.price());
        return AdminCatalogMapper.toResponse(toppingRepository.save(topping));
    }

    public AdminToppingResponse setActive(Long id, boolean active) {
        Topping topping = findTopping(id);
        topping.setActive(active);
        return AdminCatalogMapper.toResponse(toppingRepository.save(topping));
    }

    private Topping findTopping(Long id) {
        return toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topping not found: " + id));
    }
}
