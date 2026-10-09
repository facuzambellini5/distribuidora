package com.example.distribuidora.repositories;

import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.models.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
class JpaMappingTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    private final ICustomerRepository customerRepo;
    private final IOrderRepository orderRepo;

    JpaMappingTest(ICustomerRepository customerRepo, IOrderRepository orderRepo) {
        this.customerRepo = customerRepo;
        this.orderRepo = orderRepo;
    }

    @Test
    void orderMappingMatchesFlywaySchema() {
        Customer customer = new Customer();
        customer.setBusinessName("Cafe Centro");
        Customer savedCustomer = customerRepo.save(customer);

        Order order = new Order();
        order.setCustomer(savedCustomer);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddressSnapshot("San Martin 100");
        order.setTotalAmount(new BigDecimal("10.00"));

        Order saved = orderRepo.save(order);

        assertEquals(OrderStatus.PENDING, saved.getStatus());
    }
}
