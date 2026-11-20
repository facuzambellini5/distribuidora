package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.OrderDeliveryRequest;
import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.exceptions.GlobalExceptionHandler;
import com.example.distribuidora.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new OrderController(orderService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void deliveringOrderAcceptsPaymentMethodAndAmounts() throws Exception {
        mockMvc.perform(patch("/api/orders/7/delivered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"MIXED","cashAmount":12.00,"transferAmount":30.00}
                                """))
                .andExpect(status().isOk());

        verify(orderService).markDelivered(7L,
                new OrderDeliveryRequest(OrderPaymentMethod.MIXED,
                        new BigDecimal("12.00"), new BigDecimal("30.00")));
    }

    @Test
    void deliveringOrderRequiresPaymentMethod() throws Exception {
        mockMvc.perform(patch("/api/orders/7/delivered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }
}
