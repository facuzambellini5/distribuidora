package com.example.distribuidora.services;

import com.example.distribuidora.dtos.EmployeePerformanceReportResponse;
import com.example.distribuidora.dtos.SalesPeriodReportResponse;
import com.example.distribuidora.dtos.TopSellingProductReportResponse;
import com.example.distribuidora.enums.ReportGroupBy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportExportService {

    private final ReportService reportService;
    private final ReportPdfGenerator pdfGenerator;

    public ReportExportService(ReportService reportService, ReportPdfGenerator pdfGenerator) {
        this.reportService = reportService;
        this.pdfGenerator = pdfGenerator;
    }

    public byte[] exportSales(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy) {
        List<List<String>> rows = reportService.getSalesReport(fromDate, toDate, groupBy).stream()
                .map(this::salesRow)
                .toList();
        return pdfGenerator.generate("Reporte de ventas", fromDate, toDate,
                List.of("Período", "Pedidos entregados", "Ventas"), rows);
    }

    public byte[] exportTopSellingProducts(LocalDate fromDate, LocalDate toDate, int limit) {
        List<List<String>> rows = reportService.getTopSellingProducts(fromDate, toDate, limit).stream()
                .map(this::productRow)
                .toList();
        return pdfGenerator.generate("Ranking de productos más vendidos", fromDate, toDate,
                List.of("Producto", "Unidades vendidas", "Ventas"), rows);
    }

    public byte[] exportEmployeePerformance(LocalDate fromDate, LocalDate toDate, String role) {
        List<List<String>> rows = reportService.getEmployeePerformance(fromDate, toDate, role).stream()
                .map(this::employeeRow)
                .toList();
        return pdfGenerator.generate("Rendimiento por empleado", fromDate, toDate,
                List.of("Empleado", "Rol", "Pedidos", "Ventas", "Entregas"), rows);
    }

    private List<String> salesRow(SalesPeriodReportResponse report) {
        return List.of(report.period(), report.orderCount().toString(), report.salesAmount().toPlainString());
    }

    private List<String> productRow(TopSellingProductReportResponse report) {
        return List.of(report.productName(), report.quantitySold().toString(), report.salesAmount().toPlainString());
    }

    private List<String> employeeRow(EmployeePerformanceReportResponse report) {
        return List.of(report.employeeName(), report.role(), report.orderCount().toString(),
                report.salesAmount().toPlainString(), report.deliveriesCompleted().toString());
    }
}
