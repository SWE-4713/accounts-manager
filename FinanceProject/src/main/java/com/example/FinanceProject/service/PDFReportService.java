// src/main/java/com/example/FinanceProject/service/PdfReportService.java
package com.example.FinanceProject.service;

import com.example.FinanceProject.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*; // Document, Paragraph, Font, Phrase, Rectangle, Element
import com.lowagie.text.Font; // Explicit import for Font
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color; // For cell background/font colors
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PDFReportService {

    private final ObjectMapper objectMapper;
    private static final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // Define standard fonts
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.BOLD);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, Color.WHITE);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
    private static final Font FONT_BODY_RED = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.RED);
    private static final Font FONT_BODY_GREEN = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, new Color(0, 128, 0)); // Dark Green

    @Autowired
    public PDFReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private String formatAmountPdf(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        // Use parentheses for negatives
        if (amount.signum() < 0) {
            return "(" + currencyFormat.format(amount.abs()) + ")";
        } else {
            return currencyFormat.format(amount);
        }
    }
    private String formatAmountPlainPdf(BigDecimal amount) { // For TB Debits/Credits
         if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return ""; // Blank for zero
        }
         return currencyFormat.format(amount); // Format with commas/decimals
    }


    // --- PDF Generation Methods ---

    public byte[] generateTrialBalancePdf(String jsonPayload, LocalDate startDate, LocalDate endDate) throws IOException, DocumentException {
        List<TrialBalanceRow> rows = objectMapper.readValue(jsonPayload, new TypeReference<List<TrialBalanceRow>>() {});
        BigDecimal debitTotal = rows.stream().map(TrialBalanceRow::getDebit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = rows.stream().map(TrialBalanceRow::getCredit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        Document document = new Document(PageSize.LETTER); // Standard page size
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        // Add Title and Date Range
        addTitleAndDate(document, "Trial Balance", startDate, endDate);

        // Create Table
        PdfPTable table = new PdfPTable(3); // 3 columns
        table.setWidthPercentage(100); // Use full width
        table.setWidths(new float[]{4f, 2f, 2f}); // Relative widths: Name wider than amounts
        table.setSpacingBefore(15f); // Space between title and table

        // Add Headers
        addHeaderCell(table, "Account", Element.ALIGN_CENTER);
        addHeaderCell(table, "Debit", Element.ALIGN_CENTER);
        addHeaderCell(table, "Credit", Element.ALIGN_CENTER);

        // Add Data Rows
        for (TrialBalanceRow row : rows) {
            addCell(table, row.getAccountName(), Element.ALIGN_LEFT);
            addAmountCell(table, row.getDebit(), true); // Use plain format
            addAmountCell(table, row.getCredit(), true); // Use plain format
        }

        // Add Separator before Totals (optional, visual)
        addSeparatorRow(table, 3);

        // Add Totals Row
        addTotalCell(table, "Total", Element.ALIGN_LEFT);
        addTotalAmountCell(table, debitTotal, true); // Use plain format
        addTotalAmountCell(table, creditTotal, true); // Use plain format

        document.add(table);
        document.close();
        return baos.toByteArray();
    }

     public byte[] generateIncomeStatementPdf(String jsonPayload, LocalDate startDate, LocalDate endDate) throws IOException, DocumentException {
        List<IncomeStatementRow> rows = objectMapper.readValue(jsonPayload, new TypeReference<List<IncomeStatementRow>>() {});

        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        addTitleAndDate(document, "Income Statement", startDate, endDate);

        PdfPTable table = new PdfPTable(2); // Label, Amount
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 2f});
        table.setSpacingBefore(15f);

        // No headers needed for standard IS format

        for (IncomeStatementRow row : rows) {
            String label = row.getLabel();
            BigDecimal amount = row.getAmount();
            boolean isHeader = label.equalsIgnoreCase("Revenue") || label.equalsIgnoreCase("Expenses");
            boolean isTotal = label.toLowerCase().startsWith("total") || label.equalsIgnoreCase("Net Income");
            boolean isBlank = label.isEmpty();
            int align = Element.ALIGN_LEFT;
            Font font = FONT_BODY;
            float indentation = 0;

            if (isBlank) {
                 addEmptyRow(table, 2);
                 continue; // Skip rest of processing for blank rows
            }


            if (isHeader) {
                 font = FONT_BODY_BOLD;
             } else if (isTotal) {
                 font = FONT_BODY_BOLD;
                 // align = Element.ALIGN_RIGHT; // Totals often right aligned label, but standard IS keeps left
             } else {
                  // Indent sub-items
                  indentation = 15f; // Indent regular items
             }

            // Determine color for amounts based on context (simple version)
            Font amountFont = FONT_BODY;
             if (label.equalsIgnoreCase("Net Income")) {
                 amountFont = (amount != null && amount.signum() >= 0) ? FONT_BODY_GREEN : FONT_BODY_RED;
             } else if (label.toLowerCase().contains("expense")) {
                 amountFont = FONT_BODY_RED; // Assume expenses are "negative" visually
             } else if (label.toLowerCase().contains("revenue")) {
                  amountFont = FONT_BODY_GREEN; // Assume revenues are "positive" visually
             }


            // Label cell
            PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setHorizontalAlignment(align);
            labelCell.setPaddingLeft(indentation);
             labelCell.setPaddingTop(2f);
             labelCell.setPaddingBottom(4f);
            table.addCell(labelCell);

            // Amount cell
            PdfPCell amountCell = new PdfPCell(new Phrase(formatAmountPdf(amount), amountFont));
            amountCell.setBorder(Rectangle.NO_BORDER);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
             amountCell.setPaddingTop(2f);
             amountCell.setPaddingBottom(4f);
             amountCell.setPaddingRight(5f); // Padding for amount column
            table.addCell(amountCell);
        }

        document.add(table);
        document.close();
        return baos.toByteArray();
    }


    public byte[] generateBalanceSheetPdf(String jsonPayload, LocalDate date) throws IOException, DocumentException {
        List<BalanceSheetRow> rows = objectMapper.readValue(jsonPayload, new TypeReference<List<BalanceSheetRow>>() {});

        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        addTitleAndDate(document, "Balance Sheet", date, null); // Only one date for BS

        PdfPTable table = new PdfPTable(3); // Label, Amount Col 1, Amount Col 2
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 2f, 2f}); // Adjust widths as needed
        table.setSpacingBefore(15f);

        // No main headers, style applied row by row

        for (BalanceSheetRow row : rows) {
             String label = row.getLabel();
             BigDecimal amount = row.getAmount();
             String styleClass = row.getStyleClass() != null ? row.getStyleClass() : "";
             int displayColumn = row.getDisplayColumn();
             boolean showDollar = row.isShowDollarSign(); // Note: PDF doesn't inherently need '$' sign logic like HTML

             Font font = FONT_BODY;
             float indentation = 0f;
             int labelAlign = Element.ALIGN_LEFT;
             boolean isTotalOrHeader = styleClass.contains("header") || styleClass.contains("total") || styleClass.contains("final-total");


             if (styleClass.contains("header") || styleClass.contains("final-total")) {
                 font = FONT_BODY_BOLD;
             }
             if (styleClass.contains("indent-1")) {
                 indentation = 15f;
             } else if (styleClass.contains("indent-2")) {
                  indentation = 30f;
             }


            // Label Cell
            PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setHorizontalAlignment(labelAlign);
            labelCell.setPaddingLeft(indentation);
            labelCell.setPaddingBottom(4f);
             labelCell.setPaddingTop(2f);
            table.addCell(labelCell);


            // Amount Cell 1
             PdfPCell amountCell1 = new PdfPCell();
             amountCell1.setBorder(Rectangle.NO_BORDER);
             amountCell1.setHorizontalAlignment(Element.ALIGN_RIGHT);
             amountCell1.setPaddingBottom(4f);
             amountCell1.setPaddingTop(2f);
             amountCell1.setPaddingRight(5f);
             if (displayColumn == 1) {
                 amountCell1.setPhrase(new Phrase(formatAmountPdf(amount), isTotalOrHeader ? FONT_BODY_BOLD : FONT_BODY));
             } else {
                  amountCell1.setPhrase(new Phrase("")); // Empty if not col 1
             }
             table.addCell(amountCell1);

             // Amount Cell 2
             PdfPCell amountCell2 = new PdfPCell();
             amountCell2.setBorder(Rectangle.NO_BORDER);
             amountCell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
             amountCell2.setPaddingBottom(4f);
             amountCell2.setPaddingTop(2f);
             amountCell2.setPaddingRight(5f);
              if (displayColumn == 2) {
                 amountCell2.setPhrase(new Phrase(formatAmountPdf(amount), isTotalOrHeader ? FONT_BODY_BOLD : FONT_BODY));
             } else {
                  amountCell2.setPhrase(new Phrase("")); // Empty if not col 2
             }
             table.addCell(amountCell2);

            // Add underline for total rows if needed visually
            if (styleClass.contains("total") || styleClass.contains("final-total")){
                 addUnderlineRow(table, 3, indentation); // Underline below totals
             }


        }


        document.add(table);
        document.close();
        return baos.toByteArray();
    }

    public byte[] generateRetainedEarningsPdf(String jsonPayload, LocalDate startDate, LocalDate endDate) throws IOException, DocumentException {
        RetainedEarningsReport report = objectMapper.readValue(jsonPayload, RetainedEarningsReport.class);

        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        addTitleAndDate(document, "Statement of Retained Earnings", startDate, endDate);

        PdfPTable table = new PdfPTable(2); // Item, Amount
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 2f});
        table.setSpacingBefore(15f);


        // Row data
        List<Map.Entry<String, BigDecimal>> items = List.of(
            Map.entry("Retained Earnings, Beginning", report.getBeginningBalance()),
            Map.entry("Add: Net Income", report.getNetIncome()),
            Map.entry("Less: Dividends", report.getDividends()),
            Map.entry("Retained Earnings, Ending", report.getEndingBalance())
        );

        for (Map.Entry<String, BigDecimal> item : items) {
            String label = item.getKey();
            BigDecimal amount = item.getValue();
            boolean isEnding = label.contains("Ending");
            boolean isSubItem = label.trim().startsWith("Add:") || label.trim().startsWith("Less:");
            Font font = isEnding ? FONT_BODY_BOLD : FONT_BODY;
            float indentation = isSubItem ? 15f : 0f;

             // Determine color for amount
            Font amountFont = font;
            if (label.contains("Net Income")) {
                 amountFont = (amount != null && amount.signum() >= 0) ? FONT_BODY_GREEN : FONT_BODY_RED;
             } else if (label.contains("Dividends")) {
                  amountFont = FONT_BODY_RED; // Dividends reduce RE
             }


            // Label cell
            PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setPaddingLeft(indentation);
             labelCell.setPaddingBottom(4f);
             labelCell.setPaddingTop(2f);
            table.addCell(labelCell);

            // Amount cell
            PdfPCell amountCell = new PdfPCell(new Phrase(formatAmountPdf(amount), amountFont));
            amountCell.setBorder(Rectangle.NO_BORDER);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
             amountCell.setPaddingBottom(4f);
             amountCell.setPaddingTop(2f);
             amountCell.setPaddingRight(5f);
            table.addCell(amountCell);

            // Add underline before ending balance
            if (label.contains("Dividends")) {
                 addUnderlineRow(table, 2, 0f); // Underline spans full width effectively
            }
        }


        document.add(table);
        document.close();
        return baos.toByteArray();
    }


    // --- Helper Methods for PDF Table Building ---

    private void addTitleAndDate(Document document, String title, LocalDate startDate, LocalDate endDate) throws DocumentException {
        Paragraph titlePara = new Paragraph(title, FONT_TITLE);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        document.add(titlePara);

        String dateString;
        if (endDate == null || startDate.equals(endDate)) { // For BS or single date reports
            dateString = "As of " + startDate.format(dateFormatter);
        } else {
            dateString = "For the period " + startDate.format(dateFormatter) + " to " + endDate.format(dateFormatter);
        }
        Paragraph datePara = new Paragraph(dateString, FONT_SUBTITLE);
        datePara.setAlignment(Element.ALIGN_CENTER);
        datePara.setSpacingAfter(10f); // Add space after the date
        document.add(datePara);
    }

    private void addHeaderCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(Color.DARK_GRAY); // Example header color
        cell.setPadding(5);
        table.addCell(cell);
    }

     private void addCell(PdfPTable table, String text, int align) {
        addStyledCell(table, text, align, FONT_BODY);
    }
    private void addStyledCell(PdfPTable table, String text, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorder(Rectangle.NO_BORDER); // Remove cell borders for cleaner look
        table.addCell(cell);
    }


    // Specific cell adders for amounts, handling alignment and formatting
    private void addAmountCell(PdfPTable table, BigDecimal amount, boolean plainFormat) {
         String formattedAmount = plainFormat ? formatAmountPlainPdf(amount) : formatAmountPdf(amount);
         Font font = FONT_BODY;
         // Optionally apply red color for negatives if not using parentheses
         // if (!plainFormat && amount != null && amount.signum() < 0) { font = FONT_BODY_RED; }

        PdfPCell cell = new PdfPCell(new Phrase(formattedAmount, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setPaddingRight(5f); // Extra padding on right for amounts
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addTotalCell(PdfPTable table, String text, int align) {
         addStyledCell(table, text, align, FONT_BODY_BOLD);
    }
    private void addTotalAmountCell(PdfPTable table, BigDecimal amount, boolean plainFormat) {
        String formattedAmount = plainFormat ? formatAmountPlainPdf(amount) : formatAmountPdf(amount);
        Font font = FONT_BODY_BOLD; // Totals usually bold
         // Optionally apply red color for negative totals
         // if (!plainFormat && amount != null && amount.signum() < 0) { font = FONT_BODY_RED; }


        PdfPCell cell = new PdfPCell(new Phrase(formattedAmount, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setPaddingRight(5f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }


     private void addSeparatorRow(PdfPTable table, int colSpan) {
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorder(Rectangle.TOP); // Only top border to act as a line
        separatorCell.setBorderColor(Color.LIGHT_GRAY);
        separatorCell.setColspan(colSpan);
        separatorCell.setFixedHeight(1f); // Very thin line
         separatorCell.setPaddingTop(2f); // Space before line
         separatorCell.setPaddingBottom(3f); // Space after line
        table.addCell(separatorCell);
    }
     private void addUnderlineRow(PdfPTable table, int colSpan, float leftIndent) {
         // Similar to separator, but perhaps styled differently or used under specific numbers
         PdfPCell cell = new PdfPCell();
         cell.setBorder(Rectangle.TOP);
         cell.setBorderColor(Color.BLACK);
         cell.setFixedHeight(1f);
         cell.setColspan(colSpan);
         // Note: Indentation within a spanned cell is tricky. Usually applied to cell content.
         // For a simple line, it spans the columns visually.
         cell.setPaddingTop(0f);
         cell.setPaddingBottom(2f);
         table.addCell(cell);
     }
     private void addEmptyRow(PdfPTable table, int colSpan) {
        PdfPCell emptyCell = new PdfPCell(new Phrase(" ")); // Non-breaking space might be better: "\u00A0"
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setColspan(colSpan);
        emptyCell.setFixedHeight(8f); // Control spacing with height
        table.addCell(emptyCell);
    }

}
