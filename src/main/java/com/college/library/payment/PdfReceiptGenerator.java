package com.college.library.payment;

import com.college.library.tenant.TenantSettings;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReceiptGenerator {

    private static final Color NAVY = new Color(0x1B, 0x2A, 0x4A);
    private static final Color GOLD = new Color(0xB9, 0x8D, 0x2E);
    private static final Color PARCHMENT = new Color(0xF6, 0xF3, 0xEC);

    public byte[] generateReceiptPdf(Payment payment, TenantSettings settings) throws Exception {
        Document document = new Document(PageSize.A5, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);

        document.open();

        // 1. Header with Library Brand Name
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, NAVY);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        Paragraph libTitle = new Paragraph(settings.getLibraryName().toUpperCase(), headerFont);
        libTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(libTitle);

        Paragraph docTitle = new Paragraph("OFFICIAL CIRCULATION PAYMENT RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GOLD));
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.setSpacingAfter(15);
        document.add(docTitle);

        // 2. Receipt Metadata Grid
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingAfter(15);

        String receiptNo = "REC-" + payment.getId() + "-" + payment.getPaymentDate().getYear();
        String paymentTime = payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));

        addCell(metaTable, "Receipt No: " + receiptNo, regularFont, false);
        addCell(metaTable, "Date: " + paymentTime, regularFont, true);
        addCell(metaTable, "Patron: " + payment.getUser().getName(), regularFont, false);
        addCell(metaTable, "Email: " + payment.getUser().getEmail(), regularFont, true);
        addCell(metaTable, "Payment Mode: " + payment.getPaymentMethod(), regularFont, false);
        addCell(metaTable, "Reference: " + payment.getTransactionReference(), regularFont, true);

        document.add(metaTable);

        // 3. Itemized Settlement Table
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 2f, 2f});
        table.setSpacingAfter(20);

        // Header Row
        PdfPCell h1 = new PdfPCell(new Phrase("Description", boldFont));
        h1.setBackgroundColor(PARCHMENT);
        h1.setPadding(6);
        PdfPCell h2 = new PdfPCell(new Phrase("Fine ID", boldFont));
        h2.setBackgroundColor(PARCHMENT);
        h2.setPadding(6);
        PdfPCell h3 = new PdfPCell(new Phrase("Amount Paid", boldFont));
        h3.setBackgroundColor(PARCHMENT);
        h3.setPadding(6);
        h3.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(h1);
        table.addCell(h2);
        table.addCell(h3);

        String itemDesc = "Library Overdue Circulation Fine";
        String fineRef = payment.getFine() != null ? "FINE-" + payment.getFine().getId() : "GENERAL";
        if (payment.getFine() != null && payment.getFine().getTransaction() != null) {
            itemDesc += " (" + payment.getFine().getTransaction().getBook().getTitle() + ")";
        }

        PdfPCell c1 = new PdfPCell(new Phrase(itemDesc, regularFont));
        c1.setPadding(6);
        PdfPCell c2 = new PdfPCell(new Phrase(fineRef, regularFont));
        c2.setPadding(6);
        PdfPCell c3 = new PdfPCell(new Phrase("₹" + payment.getAmount().toPlainString(), boldFont));
        c3.setPadding(6);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(c1);
        table.addCell(c2);
        table.addCell(c3);

        document.add(table);

        // 4. Total & Cashier Block
        Paragraph total = new Paragraph("TOTAL SETTLED: ₹" + payment.getAmount().toPlainString(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY));
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingAfter(30);
        document.add(total);

        // Authorized Stamp Block
        PdfPTable authTable = new PdfPTable(2);
        authTable.setWidthPercentage(100);

        String cashier = payment.getRecordedBy() != null ? payment.getRecordedBy().getName() : "Online Payment Gateway";
        addCell(authTable, "Cashier / System: " + cashier, subFont, false);
        addCell(authTable, "[Digitally Verified & Sealed]", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GOLD), true);

        document.add(authTable);

        document.close();
        return baos.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        if (alignRight) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        table.addCell(cell);
    }
}
