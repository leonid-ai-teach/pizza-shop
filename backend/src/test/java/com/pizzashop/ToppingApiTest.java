package com.pizzashop;

import com.pizzashop.entity.Topping;
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
class ToppingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToppingRepository toppingRepository;

    @BeforeEach
    void setUp() {
        toppingRepository.save(new Topping("Extra Käse", "Zusätzliche Portion Mozzarella", new BigDecimal("1.00"), true));
        toppingRepository.save(new Topping("Ananas", "Ananasstücke", new BigDecimal("1.20"), false));
    }

    @Test
    void listsOnlyActiveToppings() throws Exception {
        mockMvc.perform(get("/api/toppings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Extra Käse"))
                .andExpect(jsonPath("$[0].price").value(1.00));
    }
}
