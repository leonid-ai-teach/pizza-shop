package com.pizzashop;

import com.pizzashop.entity.AdminAccess;
import com.pizzashop.entity.CustomerData;
import com.pizzashop.entity.Order;
import com.pizzashop.entity.OrderItem;
import com.pizzashop.entity.OrderStatus;
import com.pizzashop.entity.OrderType;
import com.pizzashop.entity.Pizza;
import com.pizzashop.repository.AdminAccessRepository;
import com.pizzashop.repository.OrderRepository;
import com.pizzashop.repository.PizzaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.pizzashop.AdminTestSupport.allowlistedAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(PostgresTestcontainerConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class OrderAdminApiTest {

    private static final String ADMIN = "chef@pizzashop.de";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAccessRepository adminAccessRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private EntityManager entityManager;

    private Order newOrder;
    private Order inProgressOrder;
    private Order doneOrder;

    @BeforeEach
    void setUp() {
        // No password: these tests authenticate through a RequestPostProcessor, not the login
        // endpoint, so the row only has to exist.
        adminAccessRepository.save(new AdminAccess(ADMIN, "bootstrap", null));
        Pizza pizza = pizzaRepository.save(
                new Pizza("Margherita", null, new BigDecimal("7.50"), null, true, 10));

        newOrder = createOrder(pizza, OrderStatus.NEW);
        inProgressOrder = createOrder(pizza, OrderStatus.IN_PROGRESS);
        doneOrder = createOrder(pizza, OrderStatus.DONE);
    }

    private Order createOrder(Pizza pizza, OrderStatus status) {
        CustomerData customer = new CustomerData("Mario", "Rossi", "0123", "mario@example.com",
                null, null, null, null);
        Order order = new Order(orderRepository.nextOrderNumber(), OrderType.PICKUP, customer);
        order.addItem(new OrderItem(pizza, 1, new BigDecimal("7.50"), new BigDecimal("7.50")));
        order.updateTotalPrice(new BigDecimal("7.50"));
        Order saved = orderRepository.save(order);
        if (status != OrderStatus.NEW) {
            // Walk the legal path so fixtures never bypass the rules under test.
            saved.changeStatusTo(OrderStatus.IN_PROGRESS);
            if (status == OrderStatus.DONE) {
                saved.changeStatusTo(OrderStatus.DONE);
            }
        }
        return saved;
    }

    private String statusBody(OrderStatus status) {
        return "{\"status\":\"" + status.name() + "\"}";
    }

    @Test
    void unauthenticatedAccessToAdminOrdersIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsAllOrdersForAnAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/orders").with(allowlistedAdmin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void filtersOrdersByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/orders").param("status", "IN_PROGRESS")
                        .with(allowlistedAdmin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
    }

    @Test
    void orderDetailIncludesCustomerDataAndItems() throws Exception {
        mockMvc.perform(get("/api/admin/orders/{id}", newOrder.getId()).with(allowlistedAdmin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerData.firstName").value("Mario"))
                .andExpect(jsonPath("$.items[0].pizzaName").value("Margherita"))
                .andExpect(jsonPath("$.totalPrice").value(7.50));
    }

    @Test
    void nonexistentOrderDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/orders/{id}", 999_999L).with(allowlistedAdmin(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void summaryReportsCountsPerStatus() throws Exception {
        mockMvc.perform(get("/api/admin/orders/summary").with(allowlistedAdmin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newCount").value(1))
                .andExpect(jsonPath("$.inProgressCount").value(1))
                .andExpect(jsonPath("$.doneCount").value(1));
    }

    @Test
    void advancesAnOrderFromNewToInProgress() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/{id}/status", newOrder.getId())
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(OrderStatus.IN_PROGRESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        assertThat(orderRepository.findById(newOrder.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    void cancelsAnOpenOrder() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/{id}/status", newOrder.getId())
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(OrderStatus.CANCELLED)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsSkippingStraightFromNewToDone() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/{id}/status", newOrder.getId())
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(OrderStatus.DONE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        assertThat(orderRepository.findById(newOrder.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.NEW);
    }

    @Test
    void rejectsAnyTransitionOutOfATerminalStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/{id}/status", doneOrder.getId())
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(OrderStatus.IN_PROGRESS)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void completesAnInProgressOrder() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/{id}/status", inProgressOrder.getId())
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(OrderStatus.DONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void aStaleStatusUpdateLosesToTheOneThatCommittedFirst() {
        // Two staff both loaded the order while it was still NEW. Detaching each copy models
        // the two independent requests. The first change must win, and the second must fail
        // rather than silently resurrecting an order someone already cancelled.
        Order firstWriter = orderRepository.findById(newOrder.getId()).orElseThrow();
        entityManager.detach(firstWriter);
        Order secondWriter = orderRepository.findById(newOrder.getId()).orElseThrow();
        entityManager.detach(secondWriter);

        firstWriter.changeStatusTo(OrderStatus.CANCELLED);
        orderRepository.saveAndFlush(firstWriter);

        secondWriter.changeStatusTo(OrderStatus.IN_PROGRESS);
        assertThatThrownBy(() -> orderRepository.saveAndFlush(secondWriter))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void rejectsAnUnknownStatusValue() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/{id}/status", newOrder.getId())
                        .with(allowlistedAdmin(ADMIN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }
}
