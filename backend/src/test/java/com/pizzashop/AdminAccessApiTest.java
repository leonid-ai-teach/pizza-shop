package com.pizzashop;

import com.pizzashop.entity.AdminAccess;
import com.pizzashop.repository.AdminAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.pizzashop.AdminTestSupport.allowlistedAdmin;
import static com.pizzashop.AdminTestSupport.nonAllowlistedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AdminAccessApiTest {

    private static final String EXISTING_ADMIN = "chef@pizzashop.de";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAccessRepository adminAccessRepository;

    @BeforeEach
    void setUp() {
        adminAccessRepository.save(new AdminAccess(EXISTING_ADMIN, "bootstrap"));
    }

    @Test
    void unauthenticatedRequestsToAdminApiAreRejected() throws Exception {
        mockMvc.perform(get("/api/admin/admins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedInButNonAllowlistedUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/admins").with(nonAllowlistedUser("stranger@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowlistedAdminCanListAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/admins").with(allowlistedAdmin(EXISTING_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EXISTING_ADMIN))
                .andExpect(jsonPath("$[0].approvedBy").value("bootstrap"));
    }

    @Test
    void adminCanInviteAnotherAdminWhoIsThenAllowedIn() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .with(allowlistedAdmin(EXISTING_ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newhire@pizzashop.de\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newhire@pizzashop.de"))
                .andExpect(jsonPath("$.approvedBy").value(EXISTING_ADMIN));

        assertThat(adminAccessRepository.existsById("newhire@pizzashop.de")).isTrue();
    }

    @Test
    void invitedEmailIsStoredLowercasedSoLoginCasingDoesNotLockPeopleOut() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .with(allowlistedAdmin(EXISTING_ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Mixed.Case@Pizzashop.de\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("mixed.case@pizzashop.de"));
    }

    @Test
    void invitingAnExistingAdminIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .with(allowlistedAdmin(EXISTING_ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EXISTING_ADMIN + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void invitingAnInvalidEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .with(allowlistedAdmin(EXISTING_ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void nonAllowlistedUserCannotInviteThemselves() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .with(nonAllowlistedUser("stranger@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"stranger@example.com\"}"))
                .andExpect(status().isForbidden());

        assertThat(adminAccessRepository.existsById("stranger@example.com")).isFalse();
    }

    @Test
    void meReturnsTheSignedInAdminsIdentity() throws Exception {
        mockMvc.perform(get("/api/admin/me").with(allowlistedAdmin(EXISTING_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EXISTING_ADMIN));
    }

    @Test
    void adminWritesWithoutCsrfTokenAreRejected() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .with(allowlistedAdmin(EXISTING_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newhire@pizzashop.de\"}"))
                .andExpect(status().isForbidden());
    }
}
