package com.pizzashop;

import com.pizzashop.entity.AdminAccess;
import com.pizzashop.repository.AdminAccessRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.pizzashop.AdminTestSupport.allowlistedAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(PostgresTestcontainerConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class AdminLoginApiTest {

    private static final String ADMIN = "chef@pizzashop.de";
    private static final String PASSWORD = "ein-gutes-passwort";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAccessRepository adminAccessRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        adminAccessRepository.save(
                new AdminAccess(ADMIN, "bootstrap", passwordEncoder.encode(PASSWORD)));
    }

    private String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void correctCredentialsCreateAnAdminSession() throws Exception {
        HttpSession session = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN, PASSWORD)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getRequest()
                .getSession(false);

        assertThat(session).isNotNull();

        // The session, not the request itself, is what carries the admin identity onwards.
        mockMvc.perform(get("/api/admin/me").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN));
    }

    @Test
    void loginIsCaseInsensitiveOnTheEmail() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("Chef@Pizzashop.DE", PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN, "falsches-passwort")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unknownEmailIsRejectedWithTheSameErrorAsAWrongPassword() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("stranger@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    /** A row left over from the Google-login era must not become a passwordless account. */
    @Test
    void adminWithoutAPasswordCannotSignIn() throws Exception {
        adminAccessRepository.save(new AdminAccess("legacy@pizzashop.de", "bootstrap", null));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("legacy@pizzashop.de", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingTheOwnPasswordMakesTheNewOneWorkAndTheOldOneFail() throws Exception {
        mockMvc.perform(put("/api/admin/me/password")
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD
                                + "\",\"newPassword\":\"ein-neues-passwort\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN, "ein-neues-passwort")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingThePasswordWithTheWrongCurrentOneIsRejected() throws Exception {
        mockMvc.perform(put("/api/admin/me/password")
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"falsches-passwort\","
                                + "\"newPassword\":\"ein-neues-passwort\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN, PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    void aTooShortNewPasswordIsRejected() throws Exception {
        mockMvc.perform(put("/api/admin/me/password")
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"kurz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void changingAPasswordRequiresBeingSignedIn() throws Exception {
        mockMvc.perform(put("/api/admin/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD
                                + "\",\"newPassword\":\"ein-neues-passwort\"}"))
                .andExpect(status().isUnauthorized());
    }
}
