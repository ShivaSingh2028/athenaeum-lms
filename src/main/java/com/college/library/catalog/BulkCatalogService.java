package com.college.library.catalog;

import com.college.library.entity.Book;
import com.college.library.entity.CopyCondition;
import com.college.library.repository.BookRepository;
import com.college.library.service.BookCopyService;
import com.college.library.service.BookService;
import com.college.library.tenant.TenantContext;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

@Service
public class BulkCatalogService {

    private static final Logger log = LoggerFactory.getLogger(BulkCatalogService.class);

    private final BookRepository bookRepository;
    private final BookService bookService;
    private final BookCopyService bookCopyService;

    public BulkCatalogService(BookRepository bookRepository, BookService bookService, BookCopyService bookCopyService) {
        this.bookRepository = bookRepository;
        this.bookService = bookService;
        this.bookCopyService = bookCopyService;
    }

    public BulkImportResult importCsv(Reader reader, boolean dryRun) {
        BulkImportResult result = new BulkImportResult();
        result.setDryRun(dryRun);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (CSVParser parser = new CSVParser(reader, csvFormat)) {
            int rowNum = 1; // 1-based, after header
            String tenantId = TenantContext.getTenantId();

            for (CSVRecord record : parser) {
                rowNum++;
                result.setTotalRows(result.getTotalRows() + 1);

                String isbn = record.isMapped("ISBN") ? record.get("ISBN") : (record.size() > 0 ? record.get(0) : "");
                String title = record.isMapped("Title") ? record.get("Title") : (record.size() > 1 ? record.get(1) : "");
                String author = record.isMapped("Author") ? record.get("Author") : (record.size() > 2 ? record.get(2) : "");
                String category = record.isMapped("Category") ? record.get("Category") : (record.size() > 3 ? record.get(3) : "General");
                String publisher = record.isMapped("Publisher") ? record.get("Publisher") : (record.size() > 4 ? record.get(4) : null);
                String yearStr = record.isMapped("PublicationYear") ? record.get("PublicationYear") : (record.size() > 5 ? record.get(5) : null);
                String copiesStr = record.isMapped("Copies") ? record.get("Copies") : (record.size() > 6 ? record.get(6) : "1");
                String shelfLocation = record.isMapped("ShelfLocation") ? record.get("ShelfLocation") : (record.size() > 7 ? record.get(7) : "General Rack");

                String cleanIsbn = IsbnMetadataService.normalizeIsbn(isbn);
                if (cleanIsbn.isBlank()) {
                    result.addError(rowNum, isbn, "ISBN is required and cannot be blank");
                    continue;
                }
                if (title == null || title.isBlank()) {
                    result.addError(rowNum, isbn, "Title is required");
                    continue;
                }
                if (author == null || author.isBlank()) {
                    result.addError(rowNum, isbn, "Author is required");
                    continue;
                }

                int copies = 1;
                if (copiesStr != null && !copiesStr.isBlank()) {
                    try {
                        copies = Math.max(1, Integer.parseInt(copiesStr.trim()));
                    } catch (NumberFormatException e) {
                        result.addError(rowNum, isbn, "Invalid copies count: " + copiesStr);
                        continue;
                    }
                }

                Integer publicationYear = null;
                if (yearStr != null && !yearStr.isBlank()) {
                    try {
                        publicationYear = Integer.parseInt(yearStr.trim());
                    } catch (NumberFormatException ignored) {}
                }

                if (category == null || category.isBlank()) {
                    category = "General";
                }
                if (shelfLocation == null || shelfLocation.isBlank()) {
                    shelfLocation = "General Rack";
                }

                if (!dryRun) {
                    processRowImport(tenantId, cleanIsbn, title, author, category, publisher, publicationYear, copies, shelfLocation, result);
                } else {
                    result.setSuccessfulRows(result.getSuccessfulRows() + 1);
                    result.setCopiesCreated(result.getCopiesCreated() + copies);
                }
            }
        } catch (IOException e) {
            log.error("Failed to parse CSV stream", e);
            result.addError(0, "N/A", "Failed to parse CSV: " + e.getMessage());
        }

        return result;
    }

    @Transactional
    protected void processRowImport(String tenantId, String isbn, String title, String author,
                                     String category, String publisher, Integer publicationYear,
                                     int copies, String shelfLocation, BulkImportResult result) {
        Optional<Book> existing = bookRepository.findByTenantIdAndIsbn(tenantId, isbn);
        Book targetBook;
        if (existing.isPresent()) {
            targetBook = existing.get();
        } else {
            targetBook = new Book();
            targetBook.setTenantId(tenantId);
            targetBook.setIsbn(isbn);
            targetBook.setTitle(title);
            targetBook.setAuthor(author);
            targetBook.setCategory(category);
            targetBook.setPublisher(publisher);
            targetBook.setPublicationYear(publicationYear);
            targetBook = bookRepository.save(targetBook);
            result.setBooksCreated(result.getBooksCreated() + 1);
        }

        bookCopyService.createCopies(targetBook, copies, shelfLocation, CopyCondition.GOOD);
        result.setCopiesCreated(result.getCopiesCreated() + copies);
        result.setSuccessfulRows(result.getSuccessfulRows() + 1);
    }

    public void exportCsv(Writer writer) throws IOException {
        String[] headers = {"ISBN", "Title", "Author", "Category", "Publisher", "PublicationYear", "TotalCopies", "AvailableCopies"};
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(headers).build();

        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            List<Book> books = bookService.findAll();
            for (Book b : books) {
                printer.printRecord(
                        b.getIsbn(),
                        b.getTitle(),
                        b.getAuthor(),
                        b.getCategory(),
                        b.getPublisher() != null ? b.getPublisher() : "",
                        b.getPublicationYear() != null ? b.getPublicationYear() : "",
                        b.getTotalCopies(),
                        b.getAvailableCopies()
                );
            }
            printer.flush();
        }
    }
}
