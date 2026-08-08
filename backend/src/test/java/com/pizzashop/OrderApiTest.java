package com.pizzashop;

import com.pizzashop.dto.CreateOrderRequest;
import com.pizzashop.dto.CustomerDataRequest;
import com.pizzashop.dto.OrderItemRequest;
import com.pizzashop.entity.OrderType;
import com.pizzashop.entity.Pizza;
import com.pizzashop.entity.Topping;
import com.pizzashop.repository.PizzaRepository;
import com.pizzashop.repository.ToppingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class OrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private ToppingRepository toppingRepository;

    private Pizza pizza;
    private Pizza inactivePizza;
    private Topping linkedTopping;
    private Topping unlinkedTopping;
    private Topping inactiveLinkedTopping;

    @BeforeEach
    void setUp() {
        linkedTopping = toppingRepository.save(new Topping("Extra Käse", null, new BigDecimal("1.00"), true));
        unlinkedTopping = toppingRepository.save(new Topping("Rucola", null, new BigDecimal("1.20"), true));
        inactiveLinkedTopping = toppingRepository.save(new Topping("Ananas", null, new BigDecimal("1.20"), false));

        pizza = new Pizza("Margherita", "Tomate, Mozzarella, Basilikum", new BigDecimal("7.50"), null, true, 10);
        pizza.getToppings().add(linkedTopping);
        pizza.getToppings().add(inactiveLinkedTopping);
        pizza = pizzaRepository.save(pizza);

        inactivePizza = pizzaRepository.save(
                new Pizza("Retired", null, new BigDecimal("6.00"), null, false, 20));
    }

    private CustomerDataRequest pickupCustomer() {
        return new CustomerDataRequest("Mario", "Rossi", "0123456789", "mario@example.com",
                null, null, null, null);
    }

    private CustomerDataRequest deliveryCustomer(String street) {
        return new CustomerDataRequest("Mario", "Rossi", "0123456789", "mario@example.com",
                street, "12", "12345", "Berlin");
    }

    @Test
    void createsPickupOrderWithCorrectPriceCalculationAndSnapshot() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP,
                pickupCustomer(),
                List.of(new OrderItemRequest(pizza.getId(), 2, List.of(linkedTopping.getId())))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.greaterThanOrEqualTo(100000)))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_REQUIRED"))
                .andExpect(jsonPath("$.orderType").value("PICKUP"))
                .andExpect(jsonPath("$.totalPrice").value(17.00))
                .andExpect(jsonPath("$.items[0].basePrice").value(7.50))
                .andExpect(jsonPath("$.items[0].itemTotalPrice").value(17.00))
                .andExpect(jsonPath("$.items[0].toppings[0].price").value(1.00));
    }

    @Test
    void createsDeliveryOrderRequiringAddress() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.DELIVERY,
                deliveryCustomer("Hauptstraße"),
                List.of(new OrderItemRequest(pizza.getId(), 1, List.of()))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerData.street").value("Hauptstraße"))
                .andExpect(jsonPath("$.totalPrice").value(7.50));
    }

    @Test
    void deliveryOrderWithoutAddressIsRejected() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.DELIVERY,
                deliveryCustomer(null),
                List.of(new OrderItemRequest(pizza.getId(), 1, List.of()))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void orderWithInactivePizzaIsRejected() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP,
                pickupCustomer(),
                List.of(new OrderItemRequest(inactivePizza.getId(), 1, List.of()))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void orderWithNonexistentPizzaIsRejected() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP,
                pickupCustomer(),
                List.of(new OrderItemRequest(999_999L, 1, List.of()))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void orderWithToppingNotLinkedToPizzaIsRejected() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP,
                pickupCustomer(),
                List.of(new OrderItemRequest(pizza.getId(), 1, List.of(unlinkedTopping.getId())))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void orderWithInactiveToppingIsRejected() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP,
                pickupCustomer(),
                List.of(new OrderItemRequest(pizza.getId(), 1, List.of(inactiveLinkedTopping.getId())))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void duplicateToppingIdsAreChargedOnlyOnce() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP,
                pickupCustomer(),
                List.of(new OrderItemRequest(pizza.getId(), 1, List.of(linkedTopping.getId(), linkedTopping.getId())))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].toppings.length()").value(1))
                .andExpect(jsonPath("$.totalPrice").value(8.50));
    }

    @Test
    void nullToppingIdInListIsRejectedAsValidationError() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP,
                pickupCustomer(),
                List.of(new OrderItemRequest(pizza.getId(), 1, Arrays.asList(linkedTopping.getId(), null)))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void orderWithEmptyItemsIsRejected() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(OrderType.PICKUP, pickupCustomer(), List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void orderNumbersIncreaseAcrossOrders() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP, pickupCustomer(), List.of(new OrderItemRequest(pizza.getId(), 1, List.of())));
        String json = objectMapper.writeValueAsString(request);

        String firstBody = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondBody = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long firstOrderNumber = objectMapper.readTree(firstBody).get("orderNumber").asLong();
        long secondOrderNumber = objectMapper.readTree(secondBody).get("orderNumber").asLong();

        org.assertj.core.api.Assertions.assertThat(secondOrderNumber).isGreaterThan(firstOrderNumber);
    }

    @Test
    void getOrderReturnsSnapshotPriceEvenAfterCatalogPriceChange() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP, pickupCustomer(),
                List.of(new OrderItemRequest(pizza.getId(), 1, List.of(linkedTopping.getId()))));

        String body = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("publicToken").asText();

        mockMvc.perform(get("/api/orders/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(8.50))
                .andExpect(jsonPath("$.items[0].basePrice").value(7.50));
    }

    @Test
    void nonexistentOrderReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{token}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void ordersCannotBeEnumeratedByTheirSequentialId() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                OrderType.PICKUP, pickupCustomer(),
                List.of(new OrderItemRequest(pizza.getId(), 1, List.of())));
        String body = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(body).get("id").asLong();

        // Guessing the primary key must not reveal another customer's contact details.
        mockMvc.perform(get("/api/orders/{token}", String.valueOf(orderId)))
                .andExpect(status().isNotFound());
    }
}
