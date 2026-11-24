package com.college.library.service;

import com.college.library.entity.Book;
import com.college.library.entity.Transaction;
import com.college.library.entity.TransactionStatus;
import com.college.library.repository.BookCopyRepository;
import com.college.library.repository.BookRepository;
import com.college.library.repository.TransactionRepository;
import com.college.library.repository.UserRepository;
import com.college.library.tenant.TenantContext;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public ReportService(BookRepository bookRepository,
                         BookCopyRepository bookCopyRepository,
                         UserRepository userRepository,
                         TransactionRepository transactionRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public long totalBooks() {
        return bookRepository.countByTenantId(TenantContext.getTenantId());
    }

    public long totalCopies() {
        return bookCopyRepository.count();
    }

    public long totalUsers() {
        return userRepository.countByTenantId(TenantContext.getTenantId());
    }

    public long currentlyIssuedCount() {
        return transactionRepository.countByTenantIdAndStatus(TenantContext.getTenantId(), TransactionStatus.ISSUED);
    }

    public long overdueCount() {
        return transactionRepository.findByTenantIdAndStatusAndDueDateBefore(
                TenantContext.getTenantId(), TransactionStatus.ISSUED, LocalDate.now()).size();
    }

    /** Top borrowed books for the active tenant. */
    public List<Map.Entry<Book, Long>> mostBorrowedBooks(int limit) {
        List<Transaction> all = transactionRepository.findByTenantIdOrderByIssueDateDesc(TenantContext.getTenantId());

        Map<Long, Long> countsById = all.stream()
                .filter(t -> t.getBook() != null)
                .collect(Collectors.groupingBy(t -> t.getBook().getId(), Collectors.counting()));
        Map<Long, Book> booksById = all.stream()
                .filter(t -> t.getBook() != null)
                .map(Transaction::getBook)
                .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));

        return countsById.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> Map.entry(booksById.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<Transaction> overdueList() {
        return transactionRepository.findByTenantIdAndStatusAndDueDateBefore(
                TenantContext.getTenantId(), TransactionStatus.ISSUED, LocalDate.now());
    }

    /** Never-borrowed dead stock titles */
    public List<Book> deadStockBooks() {
        String tenantId = TenantContext.getTenantId();
        List<Book> allBooks = bookRepository.findByTenantId(tenantId);
        List<Transaction> allTransactions = transactionRepository.findByTenantIdOrderByIssueDateDesc(tenantId);
        Set<Long> borrowedBookIds = allTransactions.stream()
                .filter(t -> t.getBook() != null)
                .map(t -> t.getBook().getId())
                .collect(Collectors.toSet());

        return allBooks.stream()
                .filter(b -> !borrowedBookIds.contains(b.getId()))
                .collect(Collectors.toList());
    }

    public record AgingBucket(String rangeLabel, int count, List<Transaction> transactions) {}

    public List<AgingBucket> overdueAgingAnalysis() {
        LocalDate today = LocalDate.now();
        List<Transaction> overdue = overdueList();

        List<Transaction> b1 = new ArrayList<>();
        List<Transaction> b2 = new ArrayList<>();
        List<Transaction> b3 = new ArrayList<>();

        for (Transaction tx : overdue) {
            long days = ChronoUnit.DAYS.between(tx.getDueDate(), today);
            if (days <= 7) {
                b1.add(tx);
            } else if (days <= 30) {
                b2.add(tx);
            } else {
                b3.add(tx);
            }
        }

        return List.of(
                new AgingBucket("1 - 7 Days", b1.size(), b1),
                new AgingBucket("8 - 30 Days", b2.size(), b2),
                new AgingBucket("> 30 Days (Critical)", b3.size(), b3)
        );
    }

    public byte[] generateCirculationReportPdf() throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0x1B, 0x2A, 0x4A));
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        Paragraph header = new Paragraph("ATHENAEUM CIRCULATION & AUDIT REPORT", titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        Paragraph dateLine = new Paragraph("Generated on: " + LocalDate.now() + " | Tenant: " + TenantContext.getTenantId(), subFont);
        dateLine.setAlignment(Element.ALIGN_CENTER);
        dateLine.setSpacingAfter(20);
        document.add(dateLine);

        // Stats Table
        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(15);

        String[] headers = {"Total Titles", "Total Copies", "Active Loans", "Overdue Loans"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
            cell.setBackgroundColor(new Color(0xF6, 0xF3, 0xEC));
            cell.setPadding(6);
            statsTable.addCell(cell);
        }

        statsTable.addCell(new Phrase(String.valueOf(totalBooks()), regularFont));
        statsTable.addCell(new Phrase(String.valueOf(totalCopies()), regularFont));
        statsTable.addCell(new Phrase(String.valueOf(currentlyIssuedCount()), regularFont));
        statsTable.addCell(new Phrase(String.valueOf(overdueCount()), regularFont));
        document.add(statsTable);

        // Most borrowed
        Paragraph section1 = new Paragraph("Most Borrowed Collection Titles", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(0xB9, 0x8D, 0x2E)));
        section1.setSpacingAfter(8);
        document.add(section1);

        PdfPTable borrowedTable = new PdfPTable(3);
        borrowedTable.setWidthPercentage(100);
        borrowedTable.setSpacingAfter(15);
        borrowedTable.addCell(new PdfPCell(new Phrase("Title", boldFont)));
        borrowedTable.addCell(new PdfPCell(new Phrase("Author", boldFont)));
        borrowedTable.addCell(new PdfPCell(new Phrase("Circulation Count", boldFont)));

        for (Map.Entry<Book, Long> entry : mostBorrowedBooks(5)) {
            borrowedTable.addCell(new Phrase(entry.getKey() != null ? entry.getKey().getTitle() : "Unknown", regularFont));
            borrowedTable.addCell(new Phrase(entry.getKey() != null ? entry.getKey().getAuthor() : "Unknown", regularFont));
            borrowedTable.addCell(new Phrase(String.valueOf(entry.getValue()), regularFont));
        }
        document.add(borrowedTable);

        document.close();
        return baos.toByteArray();
    }
}
