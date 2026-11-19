package com.example.distribuidora.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReportPdfGenerator {

    private static final float MARGIN = 40;
    private static final float FONT_SIZE = 10;
    private static final float LINE_HEIGHT = 14;

    public byte[] generate(String title, LocalDate fromDate, LocalDate toDate,
                           List<String> headers, List<List<String>> rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PageWriter writer = new PageWriter(document)) {
                writer.write(title, boldFont, 16);
                writer.write("Período: " + fromDate + " a " + toDate, font, FONT_SIZE);
                writer.write("", font, FONT_SIZE);
                writer.write(String.join(" | ", headers), boldFont, FONT_SIZE);
                writer.write("-".repeat(80), font, FONT_SIZE);
                if (rows.isEmpty()) {
                    writer.write("No hay resultados para el período seleccionado.", font, FONT_SIZE);
                } else {
                    for (List<String> row : rows) {
                        writer.write(String.join(" | ", row), font, FONT_SIZE);
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate the requested PDF report.", exception);
        }
    }

    private static class PageWriter implements AutoCloseable {
        private final PDDocument document;
        private PDPageContentStream content;
        private float y;

        private PageWriter(PDDocument document) throws IOException {
            this.document = document;
            startPage();
        }

        private void write(String value, PDType1Font font, float fontSize) throws IOException {
            List<String> lines = wrap(value, font, fontSize,
                    PDRectangle.A4.getWidth() - MARGIN * 2);
            for (String line : lines) {
                if (y < MARGIN) {
                    content.close();
                    startPage();
                }
                if (line.isBlank()) {
                    y -= LINE_HEIGHT;
                    continue;
                }
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(MARGIN, y);
                content.showText(line);
                content.endText();
                y -= LINE_HEIGHT;
            }
        }

        private List<String> wrap(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            if (text.isBlank()) {
                return List.of("");
            }
            StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.getStringWidth(candidate) * fontSize / 1000 > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            return lines;
        }

        private void startPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        @Override
        public void close() throws IOException {
            content.close();
        }
    }
}
