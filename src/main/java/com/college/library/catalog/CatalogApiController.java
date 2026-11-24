package com.college.library.catalog;

import com.college.library.entity.Book;
import com.college.library.entity.User;
import com.college.library.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogApiController {

    private final IsbnMetadataService isbnMetadataService;
    private final BulkCatalogService bulkCatalogService;
    private final CatalogSearchService catalogSearchService;
    private final WishlistService wishlistService;
    private final UserService userService;

    public CatalogApiController(IsbnMetadataService isbnMetadataService,
                                BulkCatalogService bulkCatalogService,
                                CatalogSearchService catalogSearchService,
                                WishlistService wishlistService,
                                UserService userService) {
        this.isbnMetadataService = isbnMetadataService;
        this.bulkCatalogService = bulkCatalogService;
        this.catalogSearchService = catalogSearchService;
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    @GetMapping("/lookup-isbn")
    public ResponseEntity<?> lookupIsbn(@RequestParam String isbn) {
        Optional<IsbnMetadataDto> metadata = isbnMetadataService.lookupIsbn(isbn);
        if (metadata.isPresent()) {
            return ResponseEntity.ok(metadata.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No bibliographic record found for ISBN: " + isbn));
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResult> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            BulkImportResult result = bulkCatalogService.importCsv(reader, dryRun);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            BulkImportResult err = new BulkImportResult();
            err.addError(0, "N/A", "Failed to process file: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/export-csv")
    public void exportCsv(HttpServletResponse response) {
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"athenaeum_catalog.csv\"");
            bulkCatalogService.exportCsv(response.getWriter());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<CatalogSearchService.SearchResult> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false, defaultValue = "newest") String sortBy) {
        return ResponseEntity.ok(catalogSearchService.search(q, category, availableOnly, sortBy));
    }

    @PostMapping("/wishlist/{bookId}")
    public ResponseEntity<?> addToWishlist(@PathVariable Long bookId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wishlist w = wishlistService.addToWishlist(user, bookId, true);
        return ResponseEntity.ok(Map.of("status", "added", "wishlistId", w.getId()));
    }

    @DeleteMapping("/wishlist/{bookId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long bookId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        wishlistService.removeFromWishlist(user.getId(), bookId);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }
}
