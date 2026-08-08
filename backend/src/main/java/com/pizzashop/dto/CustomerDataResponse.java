package com.pizzashop.dto;

public record CustomerDataResponse(
        String firstName,
        String lastName,
        String phone,
        String email,
        String street,
        String houseNumber,
        String postalCode,
        String city
) {
}
