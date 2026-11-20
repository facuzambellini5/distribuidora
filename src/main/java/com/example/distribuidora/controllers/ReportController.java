package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.EmployeePerformanceReportResponse;
import com.example.distribuidora.dtos.SalesPeriodReportResponse;
import com.example.distribuidora.dtos.TopSellingProductReportResponse;
import com.example.distribuidora.enums.ReportGroupBy;
import com.example.distribuidora.services.ReportExportService;
import com.example.distribuidora.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Reportes", description = "Reportes agregados de ventas, ranking de productos y rendimiento de empleados en JSON y PDF.")
@Validated
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportController(ReportService reportService, ReportExportService reportExportService) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    @Operation(summary = "Reporte agregado de ventas por período", description = "Requiere permiso reports:read. Fechas en formato ISO YYYY-MM-DD.")
    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('reports:read')")
    public List<SalesPeriodReportResponse> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "DAY") ReportGroupBy groupBy) {
        return reportService.getSalesReport(fromDate, toDate, groupBy);
    }

    @Operation(summary = "Ranking de productos más vendidos", description = "Requiere permiso reports:read.")
    @GetMapping("/products/top-selling")
    @PreAuthorize("hasAuthority('reports:read')")
    public List<TopSellingProductReportResponse> getTopSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.getTopSellingProducts(fromDate, toDate, limit);
    }

    @Operation(summary = "Rendimiento por empleado", description = "Requiere permiso reports:read.")
    @GetMapping("/employees/performance")
    @PreAuthorize("hasAuthority('reports:read')")
    public List<EmployeePerformanceReportResponse> getEmployeePerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String role) {
        return reportService.getEmployeePerformance(fromDate, toDate, role);
    }

    @Operation(summary = "Exportar reporte de ventas a PDF", description = "Requiere permiso reports:read.")
    @GetMapping(value = "/sales/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('reports:read')")
    public ResponseEntity<byte[]> exportSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "DAY") ReportGroupBy groupBy) {
        return pdfResponse(reportExportService.exportSales(fromDate, toDate, groupBy), "reporte-ventas.pdf");
    }

    @Operation(summary = "Exportar ranking de productos a PDF", description = "Requiere permiso reports:read.")
    @GetMapping(value = "/products/top-selling/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('reports:read')")
    public ResponseEntity<byte[]> exportTopSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {
        return pdfResponse(reportExportService.exportTopSellingProducts(fromDate, toDate, limit),
                "ranking-productos.pdf");
    }

    @Operation(summary = "Exportar rendimiento de empleados a PDF", description = "Requiere permiso reports:read.")
    @GetMapping(value = "/employees/performance/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('reports:read')")
    public ResponseEntity<byte[]> exportEmployeePerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String role) {
        return pdfResponse(reportExportService.exportEmployeePerformance(fromDate, toDate, role),
                "rendimiento-empleados.pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
