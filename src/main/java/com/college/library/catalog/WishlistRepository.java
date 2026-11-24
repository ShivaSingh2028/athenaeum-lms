package com.college.library.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByTenantIdAndUserId(String tenantId, Long userId);
    Optional<Wishlist> findByTenantIdAndUserIdAndBookId(String tenantId, Long userId, Long bookId);
    boolean existsByTenantIdAndUserIdAndBookId(String tenantId, Long userId, Long bookId);
    void deleteByTenantIdAndUserIdAndBookId(String tenantId, Long userId, Long bookId);
    List<Wishlist> findByTenantIdAndBookIdAndNotifyOnAvailableTrue(String tenantId, Long bookId);
}
