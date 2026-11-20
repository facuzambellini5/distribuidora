package com.example.distribuidora.services;

import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.models.Address;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.models.OrderItem;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.ILedgerEntryRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderReceiptPdfService {

    private static final float MARGIN = 40;
    private static final float FONT_SIZE = 10;
    private static final float LINE_HEIGHT = 15;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderService orderService;
    private final ILedgerEntryRepository ledgerRepo;

    public OrderReceiptPdfService(OrderService orderService, ILedgerEntryRepository ledgerRepo) {
        this.orderService = orderService;
        this.ledgerRepo = ledgerRepo;
    }

    // Genera el comprobante PDF de un pedido entregado sin volver a registrar pagos ni modificar estado.
    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long orderId) {
        Order order = orderService.getOrderEntity(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException(
                    "El comprobante PDF solo está disponible para pedidos entregados (estado actual: "
                            + order.getStatus() + ").");
        }

        BigDecimal totalAmount = safeAmount(order.getTotalAmount());
        BigDecimal ledgerPaid = ledgerRepo.sumPaymentsByOrderId(order.getId());
        BigDecimal orderDeliveryPaid = safeAmount(order.getCashAmount()).add(safeAmount(order.getTransferAmount()));
        BigDecimal totalPaid = (ledgerPaid != null && ledgerPaid.compareTo(BigDecimal.ZERO) > 0)
                ? ledgerPaid
                : orderDeliveryPaid;
        BigDecimal pendingBalance = totalAmount.subtract(totalPaid);
        if (pendingBalance.compareTo(BigDecimal.ZERO) < 0) {
            pendingBalance = BigDecimal.ZERO;
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        Customer customer = order.getCustomer();
        Address address = order.getDeliveryAddress();
        User responsible = order.getDeliveredBy() != null ? order.getDeliveredBy() : order.getCreatedBy();

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (ReceiptPageWriter writer = new ReceiptPageWriter(document)) {
                writer.write("COMPROBANTE DE VENTA / ENTREGA", bold, 16);
                writer.write("Distribuidora - Documento no válido como factura fiscal", regular, 9);
                writer.write("-".repeat(86), regular, FONT_SIZE);

                writer.write("Pedido: " + order.getOrderCode() + " (ID #" + order.getId() + ")", bold, 11);
                writer.write("Fecha de emisión: " + issuedAt.format(DATE_TIME_FMT), regular, FONT_SIZE);
                writer.write("Fecha de entrega: " + (order.getDeliveredAt() != null
                        ? order.getDeliveredAt().format(DATE_TIME_FMT)
                        : order.getDeliveryDate().toString()), regular, FONT_SIZE);
                writer.write("Usuario responsable: " + (responsible != null ? responsible.getFullName() : "No asignado"),
                        regular, FONT_SIZE);
                writer.write("", regular, FONT_SIZE);

                writer.write("DATOS DEL CLIENTE", bold, 11);
                writer.write("Razón social: " + (customer != null ? customer.getBusinessName() : "-"), regular, FONT_SIZE);
                if (customer != null && customer.getTaxId() != null && !customer.getTaxId().isBlank()) {
                    writer.write("CUIT / CUIL: " + customer.getTaxId(), regular, FONT_SIZE);
                }
                if (address != null) {
                    writer.write("Dirección de entrega: " + formatAddress(address), regular, FONT_SIZE);
                }
                writer.write("", regular, FONT_SIZE);

                writer.write("DETALLE DE PRODUCTOS", bold, 11);
                writer.write("Producto | Cant. | Precio Unit. | Subtotal", bold, FONT_SIZE);
                writer.write("-".repeat(86), regular, FONT_SIZE);
                for (OrderItem item : order.getItems()) {
                    String line = String.format("%s | %d | $%s | $%s",
                            item.getProduct().getName(),
                            item.getQuantity(),
                            formatMoney(item.getUnitPrice()),
                            formatMoney(item.getSubtotal()));
                    writer.write(line, regular, FONT_SIZE);
                }
                writer.write("-".repeat(86), regular, FONT_SIZE);
                writer.write("", regular, FONT_SIZE);

                writer.write("FORMA DE PAGO Y RESUMEN", bold, 11);
                writer.write("Forma de pago: " + formatPaymentMethod(order.getPaymentMethod()), regular, FONT_SIZE);
                if (order.getPaymentMethod() == OrderPaymentMethod.MIXED) {
                    writer.write("  - Efectivo: $" + formatMoney(order.getCashAmount()), regular, FONT_SIZE);
                    writer.write("  - Transferencia: $" + formatMoney(order.getTransferAmount()), regular, FONT_SIZE);
                }
                writer.write("Total del pedido: $" + formatMoney(totalAmount), bold, 11);
                writer.write("Importe pagado: $" + formatMoney(totalPaid), regular, FONT_SIZE);
                writer.write("Saldo pendiente: $" + formatMoney(pendingBalance),
                        pendingBalance.compareTo(BigDecimal.ZERO) > 0 ? bold : regular, FONT_SIZE);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el comprobante PDF del pedido " + orderId, ex);
        }
    }

    private String formatPaymentMethod(OrderPaymentMethod method) {
        if (method == null) return "Sin especificar";
        return switch (method) {
            case CASH -> "Efectivo";
            case TRANSFER -> "Transferencia";
            case MIXED -> "Mixto (Efectivo + Transferencia)";
            case CURRENT_ACCOUNT -> "Cuenta corriente";
        };
    }

    private String formatAddress(Address address) {
        String label = (address.getLabel() == null || address.getLabel().isBlank()) ? "" : address.getLabel() + " - ";
        String street = address.getStreetAddress() == null ? "" : address.getStreetAddress();
        String city = (address.getCity() == null || address.getCity().isBlank()) ? "" : ", " + address.getCity();
        return label + street + city;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatMoney(BigDecimal value) {
        return safeAmount(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static class ReceiptPageWriter implements AutoCloseable {
        private final PDDocument document;
        private PDPageContentStream content;
        private float y;

        private ReceiptPageWriter(PDDocument document) throws IOException {
            this.document = document;
            startPage();
        }

        private void write(String rawText, PDType1Font font, float fontSize) throws IOException {
            String safeText = sanitize(rawText);
            for (String line : wrap(safeText, 86)) {
                if (y < MARGIN + LINE_HEIGHT) {
                    startPage();
                }
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(MARGIN, y);
                content.showText(line);
                content.endText();
                y -= LINE_HEIGHT;
            }
        }

        private void startPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private String sanitize(String input) {
            if (input == null) return "";
            return input.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
        }

        private List<String> wrap(String text, int maxChars) {
            if (text.length() <= maxChars) {
                return List.of(text);
            }
            List<String> lines = new ArrayList<>();
            int idx = 0;
            while (idx < text.length()) {
                int end = Math.min(idx + maxChars, text.length());
                lines.add(text.substring(idx, end));
                idx = end;
            }
            return lines;
        }

        @Override
        public void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }
    }
}
