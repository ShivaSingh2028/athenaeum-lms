package com.college.library.repository;

import com.college.library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTenantId(String tenantId);

    Optional<Book> findByIdAndTenantId(Long id, String tenantId);

    Optional<Book> findByTenantIdAndIsbn(String tenantId, String isbn);

    @Query("SELECT b FROM Book b WHERE b.tenantId = :tenantId AND (" +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(b.category) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Book> searchBooks(@Param("tenantId") String tenantId, @Param("q") String query);

    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.tenantId = :tenantId ORDER BY b.category")
    List<String> findDistinctCategories(@Param("tenantId") String tenantId);

    List<Book> findByTenantIdAndCategoryIgnoreCase(String tenantId, String category);

    List<Book> findTop12ByTenantIdOrderByCreatedAtDesc(String tenantId);

    // Backward compatibility helper
    default List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(
            String title, String author, String isbn) {
        return searchBooks(com.college.library.tenant.TenantContext.getTenantId(), title);
    }

    long countByTenantId(String tenantId);
}
