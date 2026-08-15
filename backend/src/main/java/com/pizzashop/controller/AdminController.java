package com.pizzashop.controller;

import com.pizzashop.dto.AdminAccessResponse;
import com.pizzashop.dto.ChangePasswordRequest;
import com.pizzashop.dto.CurrentAdminResponse;
import com.pizzashop.dto.InviteAdminRequest;
import com.pizzashop.security.AdminPrincipals;
import com.pizzashop.service.AdminAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAccessService adminAccessService;

    public AdminController(AdminAccessService adminAccessService) {
        this.adminAccessService = adminAccessService;
    }

    /** Lets the SPA discover whether the current session is a signed-in admin. */
    @GetMapping("/me")
    public CurrentAdminResponse me(Authentication authentication) {
        return new CurrentAdminResponse(AdminPrincipals.emailOf(authentication));
    }

    /** Changes the signed-in admin's own password — never anyone else's. */
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request,
                                  Authentication authentication) {
        adminAccessService.changePassword(AdminPrincipals.emailOf(authentication),
                request.currentPassword(), request.newPassword());
    }

    @GetMapping("/admins")
    public List<AdminAccessResponse> getAdmins() {
        return adminAccessService.getAdmins();
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAccessResponse inviteAdmin(@Valid @RequestBody InviteAdminRequest request,
                                           Authentication authentication) {
        return adminAccessService.invite(request.email(), request.password(),
                AdminPrincipals.emailOf(authentication));
    }
}
