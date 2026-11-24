package com.college.library.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class IsbnMetadataService {

    private static final Logger log = LoggerFactory.getLogger(IsbnMetadataService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Cache<String, IsbnMetadataDto> cache;

    public IsbnMetadataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.cache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build();
    }

    public static String normalizeIsbn(String rawIsbn) {
        if (rawIsbn == null) return "";
        return rawIsbn.replaceAll("[^0-9X-x]", "").toUpperCase();
    }

    public void registerInCache(IsbnMetadataDto dto) {
        if (dto != null && dto.getIsbn() != null) {
            cache.put(normalizeIsbn(dto.getIsbn()), dto);
        }
    }

    public Optional<IsbnMetadataDto> lookupIsbn(String rawIsbn) {
        String isbn = normalizeIsbn(rawIsbn);
        if (isbn.length() < 10) {
            return Optional.empty();
        }

        IsbnMetadataDto cached = cache.getIfPresent(isbn);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Try Open Library first
        Optional<IsbnMetadataDto> result = fetchFromOpenLibrary(isbn);
        if (result.isEmpty()) {
            // Fallback to Google Books
            result = fetchFromGoogleBooks(isbn);
        }

        result.ifPresent(dto -> cache.put(isbn, dto));
        return result;
    }

    private Optional<IsbnMetadataDto> fetchFromOpenLibrary(String isbn) {
        try {
            String url = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "AthenaeumLMS/1.0 (academic-library-system)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode bookNode = root.get("ISBN:" + isbn);
                if (bookNode != null && bookNode.isObject()) {
                    String title = bookNode.path("title").asText("Unknown Title");
                    
                    String author = "Unknown Author";
                    JsonNode authorsNode = bookNode.path("authors");
                    if (authorsNode.isArray() && !authorsNode.isEmpty()) {
                        author = authorsNode.get(0).path("name").asText("Unknown Author");
                    }

                    String publisher = null;
                    JsonNode publishersNode = bookNode.path("publishers");
                    if (publishersNode.isArray() && !publishersNode.isEmpty()) {
                        publisher = publishersNode.get(0).path("name").asText();
                    }

                    Integer pubYear = null;
                    String pubDate = bookNode.path("publish_date").asText(null);
                    if (pubDate != null) {
                        pubYear = extractYear(pubDate);
                    }

                    String category = "General";
                    JsonNode subjectsNode = bookNode.path("subjects");
                    if (subjectsNode.isArray() && !subjectsNode.isEmpty()) {
                        category = subjectsNode.get(0).path("name").asText("General");
                    }

                    String coverUrl = null;
                    JsonNode coverNode = bookNode.path("cover");
                    if (coverNode.isObject()) {
                        if (coverNode.has("large")) {
                            coverUrl = coverNode.get("large").asText();
                        } else if (coverNode.has("medium")) {
                            coverUrl = coverNode.get("medium").asText();
                        }
                    }

                    IsbnMetadataDto dto = new IsbnMetadataDto(
                            isbn, title, author, publisher, pubYear, category, null, coverUrl
                    );
                    return Optional.of(dto);
                }
            }
        } catch (Exception e) {
            log.debug("Open Library lookup failed for ISBN {}: {}", isbn, e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<IsbnMetadataDto> fetchFromGoogleBooks(String isbn) {
        try {
            String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "AthenaeumLMS/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode items = root.path("items");
                if (items.isArray() && !items.isEmpty()) {
                    JsonNode volumeInfo = items.get(0).path("volumeInfo");
                    String title = volumeInfo.path("title").asText("Unknown Title");

                    String author = "Unknown Author";
                    JsonNode authors = volumeInfo.path("authors");
                    if (authors.isArray() && !authors.isEmpty()) {
                        author = authors.get(0).asText("Unknown Author");
                    }

                    String publisher = volumeInfo.path("publisher").asText(null);
                    Integer pubYear = extractYear(volumeInfo.path("publishedDate").asText(null));

                    String category = "General";
                    JsonNode categories = volumeInfo.path("categories");
                    if (categories.isArray() && !categories.isEmpty()) {
                        category = categories.get(0).asText("General");
                    }

                    String synopsis = volumeInfo.path("description").asText(null);
                    String coverUrl = volumeInfo.path("imageLinks").path("thumbnail").asText(null);

                    IsbnMetadataDto dto = new IsbnMetadataDto(
                            isbn, title, author, publisher, pubYear, category, synopsis, coverUrl
                    );
                    return Optional.of(dto);
                }
            }
        } catch (Exception e) {
            log.debug("Google Books lookup failed for ISBN {}: {}", isbn, e.getMessage());
        }
        return Optional.empty();
    }

    private Integer extractYear(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\b\\d{4}\\b)").matcher(dateStr);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
