package com.college.library.repository;

import com.college.library.entity.BookCopy;
import com.college.library.entity.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByTenantIdAndBarcode(String tenantId, String barcode);

    List<BookCopy> findByTenantId(String tenantId);

    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByBookIdAndTenantId(Long bookId, String tenantId);

    List<BookCopy> findByBookIdAndStatusAndTenantId(Long bookId, CopyStatus status, String tenantId);

    long countByBookIdAndStatusAndTenantId(Long bookId, CopyStatus status, String tenantId);

    long countByBookIdAndTenantId(Long bookId, String tenantId);

    long countByTenantIdAndStatus(String tenantId, CopyStatus status);

    boolean existsByTenantIdAndBarcode(String tenantId, String barcode);
}
