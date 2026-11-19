package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.EmployeePerformanceReportResponse;
import com.example.distribuidora.dtos.SalesPeriodReportResponse;
import com.example.distribuidora.dtos.TopSellingProductReportResponse;
import com.example.distribuidora.enums.ReportGroupBy;
import com.example.distribuidora.services.ReportExportService;
import com.example.distribuidora.services.ReportService;
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

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('reports:read')")
    public List<SalesPeriodReportResponse> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "DAY") ReportGroupBy groupBy) {
        return reportService.getSalesReport(fromDate, toDate, groupBy);
    }

    @GetMapping("/products/top-selling")
    @PreAuthorize("hasAuthority('reports:read')")
    public List<TopSellingProductReportResponse> getTopSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.getTopSellingProducts(fromDate, toDate, limit);
    }

    @GetMapping("/employees/performance")
    @PreAuthorize("hasAuthority('reports:read')")
    public List<EmployeePerformanceReportResponse> getEmployeePerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String role) {
        return reportService.getEmployeePerformance(fromDate, toDate, role);
    }

    @GetMapping(value = "/sales/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('reports:read')")
    public ResponseEntity<byte[]> exportSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "DAY") ReportGroupBy groupBy) {
        return pdfResponse(reportExportService.exportSales(fromDate, toDate, groupBy), "reporte-ventas.pdf");
    }

    @GetMapping(value = "/products/top-selling/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('reports:read')")
    public ResponseEntity<byte[]> exportTopSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {
        return pdfResponse(reportExportService.exportTopSellingProducts(fromDate, toDate, limit),
                "ranking-productos.pdf");
    }

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
