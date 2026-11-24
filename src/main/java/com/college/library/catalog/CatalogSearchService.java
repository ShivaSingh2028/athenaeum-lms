package com.college.library.catalog;

import com.college.library.entity.Book;
import com.college.library.repository.BookRepository;
import com.college.library.service.BookService;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogSearchService {

    private final BookRepository bookRepository;
    private final BookService bookService;

    public CatalogSearchService(BookRepository bookRepository, BookService bookService) {
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    public static class FacetCount {
        private String name;
        private long count;

        public FacetCount(String name, long count) {
            this.name = name;
            this.count = count;
        }

        public String getName() { return name; }
        public long getCount() { return count; }
    }

    public static class SearchResult {
        private List<Book> books;
        private int totalResults;
        private List<FacetCount> categoryFacets;

        public SearchResult(List<Book> books, int totalResults, List<FacetCount> categoryFacets) {
            this.books = books;
            this.totalResults = totalResults;
            this.categoryFacets = categoryFacets;
        }

        public List<Book> getBooks() { return books; }
        public int getTotalResults() { return totalResults; }
        public List<FacetCount> getCategoryFacets() { return categoryFacets; }
    }

    public SearchResult search(String query, String category, Boolean availableOnly, String sortBy) {
        String tenantId = TenantContext.getTenantId();
        List<Book> matched;

        if (query != null && !query.trim().isEmpty()) {
            matched = bookRepository.searchBooks(tenantId, query.trim());
        } else if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            matched = bookRepository.findByTenantIdAndCategoryIgnoreCase(tenantId, category.trim());
        } else {
            matched = bookRepository.findByTenantId(tenantId);
        }

        // Populate copies & availability
        bookService.populateCopyCounts(matched);

        // Filter category if both query and category provided
        if (query != null && !query.trim().isEmpty() && category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            matched = matched.stream()
                    .filter(b -> b.getCategory() != null && b.getCategory().equalsIgnoreCase(category.trim()))
                    .collect(Collectors.toList());
        }

        // Filter availability
        if (Boolean.TRUE.equals(availableOnly)) {
            matched = matched.stream()
                    .filter(b -> b.getAvailableCopies() > 0)
                    .collect(Collectors.toList());
        }

        // Sorting
        if ("newest".equalsIgnoreCase(sortBy)) {
            matched.sort(Comparator.comparing(Book::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if ("title".equalsIgnoreCase(sortBy)) {
            matched.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else if ("author".equalsIgnoreCase(sortBy)) {
            matched.sort(Comparator.comparing(Book::getAuthor, String.CASE_INSENSITIVE_ORDER));
        }

        // Category facets
        List<FacetCount> facets = getCategoryFacets(tenantId);

        return new SearchResult(matched, matched.size(), facets);
    }

    public List<Book> getNewArrivals(int limit) {
        String tenantId = TenantContext.getTenantId();
        List<Book> books = bookRepository.findTop12ByTenantIdOrderByCreatedAtDesc(tenantId);
        bookService.populateCopyCounts(books);
        return books.stream().limit(limit).collect(Collectors.toList());
    }

    public List<FacetCount> getCategoryFacets(String tenantId) {
        List<Book> allBooks = bookRepository.findByTenantId(tenantId);
        Map<String, Long> counts = allBooks.stream()
                .filter(b -> b.getCategory() != null && !b.getCategory().isBlank())
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> new FacetCount(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }
}
