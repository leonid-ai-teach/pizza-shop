package com.pizzashop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerDataRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        @NotBlank @Email String email,
        String street,
        String houseNumber,
        String postalCode,
        String city
) {
}
