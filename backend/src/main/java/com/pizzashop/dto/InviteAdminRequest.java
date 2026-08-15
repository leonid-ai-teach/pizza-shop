package com.pizzashop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteAdminRequest(
        @NotBlank @Email String email,
        // The inviting admin picks the initial password and passes it on out of band; the invited
        // person changes it afterwards. A minimum length is the one guard worth having here.
        @NotBlank @Size(min = 10, max = 100) String password
) {
}
