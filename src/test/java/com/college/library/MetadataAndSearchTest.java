package com.college.library;

import com.college.library.catalog.*;
import com.college.library.entity.Book;
import com.college.library.entity.BookCopy;
import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.fake.*;
import com.college.library.service.BookCopyService;
import com.college.library.service.BookService;
import com.college.library.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataAndSearchTest {

    private static final String TENANT_ID = "t-test-univ";

    private FakeBookRepository bookRepo;
    private FakeBookCopyRepository copyRepo;
    private FakeWishlistRepository wishlistRepo;
    private FakeBookRequestRepository requestRepo;
    private FakeNotificationRepository notificationRepo;

    private BookCopyService bookCopyService;
    private BookService bookService;
    private IsbnMetadataService isbnMetadataService;
    private BulkCatalogService bulkCatalogService;
    private CatalogSearchService catalogSearchService;
    private WishlistService wishlistService;
    private BookRequestService bookRequestService;

    private User sampleUser;

    @BeforeEach
    public void setup() {
        TenantContext.setTenantId(TENANT_ID);

        bookRepo = new FakeBookRepository();
        copyRepo = new FakeBookCopyRepository();
        wishlistRepo = new FakeWishlistRepository();
        requestRepo = new FakeBookRequestRepository();
        notificationRepo = new FakeNotificationRepository();

        bookCopyService = new BookCopyService(copyRepo);
        bookService = new BookService(bookRepo, bookCopyService);
        isbnMetadataService = new IsbnMetadataService(new ObjectMapper());
        bulkCatalogService = new BulkCatalogService(bookRepo, bookService, bookCopyService);
        catalogSearchService = new CatalogSearchService(bookRepo, bookService);
        wishlistService = new WishlistService(wishlistRepo, bookRepo, notificationRepo);
        bookRequestService = new BookRequestService(requestRepo, notificationRepo);

        sampleUser = new User("Ada Lovelace", "ada@example.edu", "hashpass", Role.MEMBER);
        sampleUser.setId(101L);
        sampleUser.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void teardown() {
        TenantContext.clear();
    }

    @Test
    public void testIsbnNormalizationAndCacheLookup() {
        String rawIsbn = " 978-0-13-468599-1 ";
        String normalized = IsbnMetadataService.normalizeIsbn(rawIsbn);
        assertEquals("9780134685991", normalized);

        // Preload metadata into cache
        IsbnMetadataDto dto = new IsbnMetadataDto(
                "9780134685991",
                "Effective Java",
                "Joshua Bloch",
                "Addison-Wesley",
                2018,
                "Computer Science",
                "The definitive guide to Java platform best practices.",
                "https://covers.openlibrary.org/b/id/8231996-L.jpg"
        );
        isbnMetadataService.registerInCache(dto);

        Optional<IsbnMetadataDto> retrieved = isbnMetadataService.lookupIsbn(rawIsbn);
        assertTrue(retrieved.isPresent(), "Metadata should be found via normalized cache");
        assertEquals("Effective Java", retrieved.get().getTitle());
        assertEquals("Joshua Bloch", retrieved.get().getAuthor());
        assertEquals(2018, retrieved.get().getPublicationYear());
        assertEquals("Computer Science", retrieved.get().getCategory());
    }

    @Test
    public void testBulkCsvImportDryRun() {
        String csvData = """
                ISBN,Title,Author,Category,Publisher,PublicationYear,Copies,ShelfLocation
                9780132350884,Clean Code,Robert C. Martin,Software Engineering,Prentice Hall,2008,3,Rack A-1
                9780201633610,Design Patterns,Erich Gamma et al,Software Architecture,Addison-Wesley,1994,2,Rack A-2
                9780000000000,,Missing Title Author,General,,,1,
                """;

        BulkImportResult dryRunResult = bulkCatalogService.importCsv(new StringReader(csvData), true);

        assertTrue(dryRunResult.isDryRun());
        assertEquals(3, dryRunResult.getTotalRows());
        assertEquals(2, dryRunResult.getSuccessfulRows());
        assertEquals(1, dryRunResult.getErrorRows());
        assertEquals(5, dryRunResult.getCopiesCreated());
        assertEquals("Title is required", dryRunResult.getErrors().get(0).getMessage());

        // In dry run, database must remain untouched
        assertEquals(0, bookRepo.count(), "Dry-run must not persist books");
        assertEquals(0, copyRepo.count(), "Dry-run must not persist copies");
    }

    @Test
    public void testBulkCsvImportExecutionAndExport() throws Exception {
        String csvData = """
                ISBN,Title,Author,Category,Publisher,PublicationYear,Copies,ShelfLocation
                9780132350884,Clean Code,Robert C. Martin,Software Engineering,Prentice Hall,2008,2,Rack A-1
                9780201633610,Design Patterns,Erich Gamma et al,Software Architecture,Addison-Wesley,1994,3,Rack A-2
                """;

        BulkImportResult result = bulkCatalogService.importCsv(new StringReader(csvData), false);

        assertFalse(result.isDryRun());
        assertEquals(2, result.getSuccessfulRows());
        assertEquals(0, result.getErrorRows());
        assertEquals(2, result.getBooksCreated());
        assertEquals(5, result.getCopiesCreated());

        assertEquals(2, bookRepo.count());
        assertEquals(5, copyRepo.count());

        // Verify CSV export
        StringWriter writer = new StringWriter();
        bulkCatalogService.exportCsv(writer);
        String exported = writer.toString();

        assertTrue(exported.contains("Clean Code"));
        assertTrue(exported.contains("Design Patterns"));
        assertTrue(exported.contains("9780132350884"));
    }

    @Test
    public void testCatalogSearchAndFacets() {
        Book b1 = new Book();
        b1.setTitle("Introduction to Algorithms");
        b1.setAuthor("Thomas H. Cormen");
        b1.setIsbn("9780262033848");
        b1.setCategory("Computer Science");
        b1.setTenantId(TENANT_ID);
        b1.setTotalCopies(3);
        bookService.addBook(b1);

        Book b2 = new Book();
        b2.setTitle("A Brief History of Time");
        b2.setAuthor("Stephen Hawking");
        b2.setIsbn("9780553380163");
        b2.setCategory("Physics");
        b2.setTenantId(TENANT_ID);
        b2.setTotalCopies(1);
        bookService.addBook(b2);

        // Search query
        CatalogSearchService.SearchResult searchResult = catalogSearchService.search("algorithms", null, null, "newest");
        assertEquals(1, searchResult.getTotalResults());
        assertEquals("Introduction to Algorithms", searchResult.getBooks().get(0).getTitle());

        // Category Facets
        List<CatalogSearchService.FacetCount> facets = searchResult.getCategoryFacets();
        assertTrue(facets.stream().anyMatch(f -> f.getName().equals("Computer Science") && f.getCount() == 1));
        assertTrue(facets.stream().anyMatch(f -> f.getName().equals("Physics") && f.getCount() == 1));

        // New Arrivals
        List<Book> arrivals = catalogSearchService.getNewArrivals(5);
        assertEquals(2, arrivals.size());
    }

    @Test
    public void testWishlistAndAvailabilityNotification() {
        Book book = new Book();
        book.setTitle("Structure and Interpretation of Computer Programs");
        book.setAuthor("Harold Abelson");
        book.setIsbn("9780262510875");
        book.setCategory("Computer Science");
        book.setTenantId(TENANT_ID);
        book.setTotalCopies(1);
        book = bookService.addBook(book);

        // Add to wishlist
        Wishlist w = wishlistService.addToWishlist(sampleUser, book.getId(), true);
        assertNotNull(w.getId());
        assertTrue(wishlistService.isWishlisted(sampleUser.getId(), book.getId()));

        // Trigger notification check when a copy becomes available
        wishlistService.notifyPatronsWhenBookAvailable(book.getId());

        List<com.college.library.entity.Notification> notifications = notificationRepo.findByTenantIdAndUserOrderByCreatedAtDesc(TENANT_ID, sampleUser);
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).getTitle().contains("Wishlist"));
        assertTrue(notifications.get(0).getMessage().contains("Structure and Interpretation"));

        // Remove from wishlist
        wishlistService.removeFromWishlist(sampleUser.getId(), book.getId());
        assertFalse(wishlistService.isWishlisted(sampleUser.getId(), book.getId()));
    }

    @Test
    public void testBookAcquisitionRequestLifecycle() {
        // Patron submits request
        BookRequest req = bookRequestService.submitRequest(
                sampleUser,
                "Designing Data-Intensive Applications",
                "Martin Kleppmann",
                "9781449373320",
                "O'Reilly Media",
                "Core syllabus reference for Distributed Systems course"
        );

        assertNotNull(req.getId());
        assertEquals(BookRequestStatus.PENDING, req.getStatus());
        assertEquals(1, bookRequestService.getUserRequests(sampleUser.getId()).size());

        // Librarian reviews and approves
        User librarian = new User("Grace Hopper", "grace@example.edu", "pass", Role.LIBRARIAN);
        librarian.setId(201L);
        librarian.setTenantId(TENANT_ID);

        BookRequest reviewed = bookRequestService.reviewRequest(
                req.getId(), librarian, BookRequestStatus.APPROVED, "Approved for purchase under Q2 budget."
        );

        assertEquals(BookRequestStatus.APPROVED, reviewed.getStatus());
        assertEquals("Approved for purchase under Q2 budget.", reviewed.getAdminNotes());

        // Check patron notification
        List<com.college.library.entity.Notification> notifications = notificationRepo.findByTenantIdAndUserOrderByCreatedAtDesc(TENANT_ID, sampleUser);
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).getMessage().contains("approved"));
    }
}
