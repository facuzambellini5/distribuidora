package com.example.distribuidora.services;

import com.example.distribuidora.dtos.EmployeePerformanceReportResponse;
import com.example.distribuidora.dtos.SalesPeriodReportResponse;
import com.example.distribuidora.dtos.TopSellingProductReportResponse;
import com.example.distribuidora.enums.ReportGroupBy;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.projections.EmployeeDeliveryProjection;
import com.example.distribuidora.repositories.projections.EmployeeSalesProjection;
import com.example.distribuidora.repositories.projections.SalesPeriodProjection;
import com.example.distribuidora.repositories.projections.TopSellingProductProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final IOrderRepository orderRepository;

    public ReportService(IOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Suma todos los pedidos entregados (incluidos los cobrados por cuenta corriente) filtrando por deliveredAt.
    @Transactional(readOnly = true)
    public List<SalesPeriodReportResponse> getSalesReport(LocalDate fromDate, LocalDate toDate,
                                                           ReportGroupBy groupBy) {
        validatePeriod(fromDate, toDate);
        ReportGroupBy effectiveGroupBy = groupBy != null ? groupBy : ReportGroupBy.DAY;
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay();

        List<SalesPeriodReportResponse> results = orderRepository.summarizeSales(
                        fromDateTime, toDateTime, fromDate.toString(), toDate.toString(), effectiveGroupBy.name())
                .stream()
                .map(this::toSalesResponse)
                .toList();

        if (results.isEmpty() && effectiveGroupBy == ReportGroupBy.DAY && fromDate.equals(toDate)) {
            return List.of(new SalesPeriodReportResponse(fromDate.toString(), 0L, BigDecimal.ZERO));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<TopSellingProductReportResponse> getTopSellingProducts(LocalDate fromDate, LocalDate toDate,
                                                                        int limit) {
        validatePeriod(fromDate, toDate);
        if (limit < 1 || limit > 100) {
            throw new BusinessRuleException("The product report limit must be between 1 and 100.");
        }
        return orderRepository.findTopSellingProducts(fromDate, toDate, limit).stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeePerformanceReportResponse> getEmployeePerformance(LocalDate fromDate, LocalDate toDate,
                                                                           String role) {
        validatePeriod(fromDate, toDate);

        Map<Long, EmployeePerformance> performanceByEmployee = new LinkedHashMap<>();
        for (EmployeeSalesProjection sales : orderRepository.summarizeEmployeeSales(fromDate, toDate, role)) {
            performanceByEmployee.put(sales.getEmployeeId(), new EmployeePerformance(
                    sales.getEmployeeId(), sales.getEmployeeName(), sales.getRole(), sales.getOrderCount(),
                    sales.getSalesAmount(), 0L));
        }
        for (EmployeeDeliveryProjection delivery : orderRepository.summarizeEmployeeDeliveries(fromDate, toDate, role)) {
            performanceByEmployee.compute(delivery.getEmployeeId(), (id, current) -> {
                if (current == null) {
                    return new EmployeePerformance(id, delivery.getEmployeeName(), delivery.getRole(), 0L,
                            BigDecimal.ZERO, delivery.getDeliveriesCompleted());
                }
                current.deliveriesCompleted = delivery.getDeliveriesCompleted();
                return current;
            });
        }

        return performanceByEmployee.values().stream()
                .map(EmployeePerformance::toResponse)
                .sorted(Comparator.comparing(EmployeePerformanceReportResponse::employeeName,
                        String.CASE_INSENSITIVE_ORDER).thenComparing(EmployeePerformanceReportResponse::employeeId))
                .toList();
    }

    private void validatePeriod(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("The report start date must be on or before its end date.");
        }
    }

    private SalesPeriodReportResponse toSalesResponse(SalesPeriodProjection projection) {
        return new SalesPeriodReportResponse(
                projection.getPeriod(),
                projection.getOrderCount() != null ? projection.getOrderCount() : 0L,
                projection.getSalesAmount() != null ? projection.getSalesAmount() : BigDecimal.ZERO
        );
    }

    private TopSellingProductReportResponse toProductResponse(TopSellingProductProjection projection) {
        return new TopSellingProductReportResponse(projection.getProductId(), projection.getProductName(),
                projection.getQuantitySold(), projection.getSalesAmount());
    }

    private static class EmployeePerformance {
        private final Long employeeId;
        private final String employeeName;
        private final String role;
        private final Long orderCount;
        private final BigDecimal salesAmount;
        private Long deliveriesCompleted;

        private EmployeePerformance(Long employeeId, String employeeName, String role, Long orderCount,
                                    BigDecimal salesAmount, Long deliveriesCompleted) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.role = role;
            this.orderCount = orderCount;
            this.salesAmount = salesAmount;
            this.deliveriesCompleted = deliveriesCompleted;
        }

        private EmployeePerformanceReportResponse toResponse() {
            return new EmployeePerformanceReportResponse(employeeId, employeeName, role, orderCount,
                    salesAmount, deliveriesCompleted);
        }
    }
}
