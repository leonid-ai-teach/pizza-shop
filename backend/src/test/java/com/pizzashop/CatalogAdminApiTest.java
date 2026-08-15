package com.pizzashop;

import com.pizzashop.entity.AdminAccess;
import com.pizzashop.entity.Pizza;
import com.pizzashop.entity.Topping;
import com.pizzashop.repository.AdminAccessRepository;
import com.pizzashop.repository.PizzaRepository;
import com.pizzashop.repository.ToppingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.pizzashop.AdminTestSupport.allowlistedAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(PostgresTestcontainerConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class CatalogAdminApiTest {

    private static final String ADMIN = "chef@pizzashop.de";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAccessRepository adminAccessRepository;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private ToppingRepository toppingRepository;

    private Pizza activePizza;
    private Pizza inactivePizza;
    private Topping cheese;
    private Topping salami;

    @BeforeEach
    void setUp() {
        // No password: these tests authenticate through a RequestPostProcessor, not the login
        // endpoint, so the row only has to exist.
        adminAccessRepository.save(new AdminAccess(ADMIN, "bootstrap", null));

        cheese = toppingRepository.save(new Topping("Extra Käse", null, new BigDecimal("1.00"), true));
        salami = toppingRepository.save(new Topping("Salami", null, new BigDecimal("1.50"), true));
        toppingRepository.save(new Topping("Ananas", null, new BigDecimal("1.20"), false));

        activePizza = new Pizza("Margherita", "Klassisch", new BigDecimal("7.50"), null, true, 10);
        activePizza.getToppings().add(cheese);
        activePizza = pizzaRepository.save(activePizza);

        inactivePizza = pizzaRepository.save(
                new Pizza("Retired", null, new BigDecimal("6.00"), null, false, 20));
    }

    // --- access control ---

    @Test
    void unauthenticatedCatalogAdminAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/pizzas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/toppings")).andExpect(status().isUnauthorized());
    }

    // --- pizzas ---

    @Test
    void adminPizzaListIncludesInactivePizzasUnlikeThePublicMenu() throws Exception {
        mockMvc.perform(get("/api/admin/pizzas").with(allowlistedAdmin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name=='Retired')].active").value(false));
    }

    @Test
    void createsAPizza() throws Exception {
        mockMvc.perform(post("/api/admin/pizzas")
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tonno","description":"Thunfisch","price":9.50,
                                 "imagePath":"https://example.com/tonno.jpg","sortOrder":30,
                                 "toppingIds":[%d]}
                                """.formatted(salami.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tonno"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.toppings.length()").value(1));
    }

    @Test
    void rejectsAPizzaWithANegativePrice() throws Exception {
        mockMvc.perform(post("/api/admin/pizzas")
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"price\":-1,\"sortOrder\":1,\"toppingIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void updatesPizzaFieldsAndReconcilesToppingAssignment() throws Exception {
        mockMvc.perform(put("/api/admin/pizzas/{id}", activePizza.getId())
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Margherita Speciale","description":"Neu","price":8.25,
                                 "imagePath":null,"sortOrder":15,"toppingIds":[%d]}
                                """.formatted(salami.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Margherita Speciale"))
                .andExpect(jsonPath("$.price").value(8.25))
                .andExpect(jsonPath("$.sortOrder").value(15))
                // cheese was removed and salami added, not merely appended
                .andExpect(jsonPath("$.toppings.length()").value(1))
                .andExpect(jsonPath("$.toppings[0].name").value("Salami"));
    }

    @Test
    void rejectsUpdatingAPizzaWithAnUnknownTopping() throws Exception {
        mockMvc.perform(put("/api/admin/pizzas/{id}", activePizza.getId())
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Margherita","description":null,"price":7.50,
                                 "imagePath":null,"sortOrder":10,"toppingIds":[999999]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void deactivatesAPizzaWithoutDeletingIt() throws Exception {
        mockMvc.perform(patch("/api/admin/pizzas/{id}/active", activePizza.getId())
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(pizzaRepository.findById(activePizza.getId())).isPresent();
        // and it disappears from the customer-facing menu
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(jsonPath("$[?(@.name=='Margherita')]").isEmpty());
    }

    @Test
    void reactivatesAPizza() throws Exception {
        mockMvc.perform(patch("/api/admin/pizzas/{id}/active", inactivePizza.getId())
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updatingAnUnknownPizzaReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/admin/pizzas/{id}", 999_999L)
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","description":null,"price":1.00,
                                 "imagePath":null,"sortOrder":1,"toppingIds":[]}
                                """))
                .andExpect(status().isNotFound());
    }

    // --- toppings ---

    @Test
    void adminToppingListIncludesInactiveToppings() throws Exception {
        mockMvc.perform(get("/api/admin/toppings").with(allowlistedAdmin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.name=='Ananas')].active").value(false));
    }

    @Test
    void createsATopping() throws Exception {
        mockMvc.perform(post("/api/admin/toppings")
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rucola\",\"description\":\"Frisch\",\"price\":1.20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rucola"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updatesAToppingPrice() throws Exception {
        mockMvc.perform(put("/api/admin/toppings/{id}", cheese.getId())
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Extra Käse\",\"description\":\"Mehr\",\"price\":1.30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(1.30));
    }

    @Test
    void deactivatesAToppingWithoutDeletingIt() throws Exception {
        mockMvc.perform(patch("/api/admin/toppings/{id}/active", cheese.getId())
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(toppingRepository.findById(cheese.getId())).isPresent();
    }

    @Test
    void aPriceChangeDoesNotAffectPricesAlreadySnapshottedOnAnOrder() throws Exception {
        String orderJson = """
                {"orderType":"PICKUP",
                 "customerData":{"firstName":"Mario","lastName":"Rossi","phone":"0123",
                                 "email":"mario@example.com"},
                 "items":[{"pizzaId":%d,"quantity":1,"toppingIds":[%d]}]}
                """.formatted(activePizza.getId(), cheese.getId());

        String body = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"publicToken\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/admin/pizzas/{id}", activePizza.getId())
                        .with(allowlistedAdmin(ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Margherita","description":null,"price":99.00,
                                 "imagePath":null,"sortOrder":10,"toppingIds":[%d]}
                                """.formatted(cheese.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(8.50))
                .andExpect(jsonPath("$.items[0].basePrice").value(7.50));
    }
}
