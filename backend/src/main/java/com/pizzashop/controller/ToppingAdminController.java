package com.pizzashop.controller;

import com.pizzashop.dto.AdminToppingResponse;
import com.pizzashop.dto.ToppingWriteRequest;
import com.pizzashop.dto.UpdateActiveRequest;
import com.pizzashop.service.ToppingAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/toppings")
public class ToppingAdminController {

    private final ToppingAdminService toppingAdminService;

    public ToppingAdminController(ToppingAdminService toppingAdminService) {
        this.toppingAdminService = toppingAdminService;
    }

    @GetMapping
    public List<AdminToppingResponse> getToppings() {
        return toppingAdminService.getToppings();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminToppingResponse createTopping(@Valid @RequestBody ToppingWriteRequest request) {
        return toppingAdminService.createTopping(request);
    }

    @PutMapping("/{id}")
    public AdminToppingResponse updateTopping(@PathVariable Long id,
                                              @Valid @RequestBody ToppingWriteRequest request) {
        return toppingAdminService.updateTopping(id, request);
    }

    @PatchMapping("/{id}/active")
    public AdminToppingResponse setActive(@PathVariable Long id,
                                          @Valid @RequestBody UpdateActiveRequest request) {
        return toppingAdminService.setActive(id, request.active());
    }
}
