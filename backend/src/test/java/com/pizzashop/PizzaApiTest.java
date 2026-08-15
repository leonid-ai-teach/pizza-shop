package com.pizzashop;

import com.pizzashop.entity.Pizza;
import com.pizzashop.entity.Topping;
import com.pizzashop.repository.PizzaRepository;
import com.pizzashop.repository.ToppingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(PostgresTestcontainerConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class PizzaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private ToppingRepository toppingRepository;

    private Topping activeTopping1;
    private Topping activeTopping2;
    private Topping inactiveTopping;
    private Pizza activePizza;
    private Pizza inactivePizza;

    @BeforeEach
    void setUp() {
        activeTopping1 = toppingRepository.save(
                new Topping("Extra Käse", "Zusätzliche Portion Mozzarella", new BigDecimal("1.00"), true));
        activeTopping2 = toppingRepository.save(
                new Topping("Salami", "Würzige Salami", new BigDecimal("1.50"), true));
        inactiveTopping = toppingRepository.save(
                new Topping("Ananas", "Ananasstücke", new BigDecimal("1.20"), false));

        activePizza = new Pizza("Margherita", "Tomate, Mozzarella, Basilikum",
                new BigDecimal("7.50"), null, true, 10);
        activePizza.getToppings().add(activeTopping1);
        activePizza.getToppings().add(activeTopping2);
        activePizza.getToppings().add(inactiveTopping);
        activePizza = pizzaRepository.save(activePizza);

        inactivePizza = pizzaRepository.save(
                new Pizza("Retired Pizza", "No longer offered", new BigDecimal("6.00"), null, false, 20));
    }

    @Test
    void listsOnlyActivePizzasSortedBySortOrder() throws Exception {
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activePizza.getId()))
                .andExpect(jsonPath("$[0].name").value("Margherita"))
                .andExpect(jsonPath("$[0].price").value(7.50));
    }

    @Test
    void pizzaDetailIncludesOnlyActiveLinkedToppings() throws Exception {
        mockMvc.perform(get("/api/pizzas/{id}", activePizza.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Margherita"))
                .andExpect(jsonPath("$.toppings.length()").value(2))
                .andExpect(jsonPath("$.toppings[*].name")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("Extra Käse", "Salami")));
    }

    @Test
    void inactivePizzaDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/pizzas/{id}", inactivePizza.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void nonexistentPizzaDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/pizzas/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void nonNumericPizzaIdReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/pizzas/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
