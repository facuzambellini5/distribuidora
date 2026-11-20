package com.example.distribuidora.controllers;

import com.example.distribuidora.services.DeliveryStockService;
import com.example.distribuidora.services.OrderService;
import com.example.distribuidora.services.TruckService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TruckControllerTest {

    private final TruckService truckService = mock(TruckService.class);
    private final DeliveryStockService stockService = mock(DeliveryStockService.class);
    private final OrderService orderService = mock(OrderService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TruckController(truckService, stockService, orderService))
            .build();

    @Test
    void listsOrdersAtTruckScopedEndpoint() throws Exception {
        when(orderService.getOrdersByTruck(3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/trucks/3/orders"))
                .andExpect(status().isOk());

        verify(orderService).getOrdersByTruck(3L);
    }

    @Test
    void exposesActiveTrucksUsingStandardResourcePath() throws Exception {
        when(truckService.getActiveTrucks()).thenReturn(List.of());

        mockMvc.perform(get("/api/trucks").param("active", "true"))
                .andExpect(status().isOk());

        verify(truckService).getActiveTrucks();
    }
}
