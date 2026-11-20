package com.example.distribuidora.services;

import com.example.distribuidora.dtos.SalesPeriodReportResponse;
import com.example.distribuidora.enums.ReportGroupBy;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.projections.SalesPeriodProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private IOrderRepository orderRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getSalesReportForSingleDaySumsDeliveredOrdersByDeliveredAt() {
        LocalDate today = LocalDate.of(2026, 11, 19);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        SalesPeriodProjection projection = new SalesPeriodProjection() {
            @Override
            public String getPeriod() {
                return "2026-11-19";
            }

            @Override
            public Long getOrderCount() {
                return 3L;
            }

            @Override
            public BigDecimal getSalesAmount() {
                return new BigDecimal("125000.00");
            }
        };

        when(orderRepository.summarizeSales(start, end, "2026-11-19", "2026-11-19", "DAY"))
                .thenReturn(List.of(projection));

        List<SalesPeriodReportResponse> result = reportService.getSalesReport(today, today, ReportGroupBy.DAY);

        assertEquals(1, result.size());
        assertEquals("2026-11-19", result.get(0).period());
        assertEquals(3L, result.get(0).orderCount());
        assertEquals(new BigDecimal("125000.00"), result.get(0).salesAmount());
        verify(orderRepository).summarizeSales(start, end, "2026-11-19", "2026-11-19", "DAY");
    }

    @Test
    void getSalesReportForSingleDayWithNoDeliveriesReturnsZeroEntry() {
        LocalDate today = LocalDate.of(2026, 11, 19);
        when(orderRepository.summarizeSales(today.atStartOfDay(), today.plusDays(1).atStartOfDay(),
                "2026-11-19", "2026-11-19", "DAY"))
                .thenReturn(List.of());

        List<SalesPeriodReportResponse> result = reportService.getSalesReport(today, today, ReportGroupBy.DAY);

        assertEquals(1, result.size());
        assertEquals("2026-11-19", result.get(0).period());
        assertEquals(0L, result.get(0).orderCount());
        assertEquals(BigDecimal.ZERO, result.get(0).salesAmount());
    }
}
