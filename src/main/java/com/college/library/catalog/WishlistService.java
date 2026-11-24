package com.college.library.catalog;

import com.college.library.entity.Book;
import com.college.library.entity.Notification;
import com.college.library.entity.User;
import com.college.library.repository.BookRepository;
import com.college.library.repository.NotificationRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final NotificationRepository notificationRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           BookRepository bookRepository,
                           NotificationRepository notificationRepository) {
        this.wishlistRepository = wishlistRepository;
        this.bookRepository = bookRepository;
        this.notificationRepository = notificationRepository;
    }

    public List<Wishlist> getUserWishlist(Long userId) {
        return wishlistRepository.findByTenantIdAndUserId(TenantContext.getTenantId(), userId);
    }

    public boolean isWishlisted(Long userId, Long bookId) {
        return wishlistRepository.existsByTenantIdAndUserIdAndBookId(TenantContext.getTenantId(), userId, bookId);
    }

    @Transactional
    public Wishlist addToWishlist(User user, Long bookId, boolean notifyOnAvailable) {
        String tenantId = TenantContext.getTenantId();
        return wishlistRepository.findByTenantIdAndUserIdAndBookId(tenantId, user.getId(), bookId)
                .orElseGet(() -> {
                    Book book = bookRepository.findByIdAndTenantId(bookId, tenantId)
                            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
                    Wishlist w = new Wishlist(user, book, notifyOnAvailable);
                    return wishlistRepository.save(w);
                });
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long bookId) {
        wishlistRepository.deleteByTenantIdAndUserIdAndBookId(TenantContext.getTenantId(), userId, bookId);
    }

    @Transactional
    public void notifyPatronsWhenBookAvailable(Long bookId) {
        String tenantId = TenantContext.getTenantId();
        List<Wishlist> notifyList = wishlistRepository.findByTenantIdAndBookIdAndNotifyOnAvailableTrue(tenantId, bookId);
        if (notifyList.isEmpty()) return;

        for (Wishlist w : notifyList) {
            Notification n = new Notification(
                    w.getUser(),
                    "Wishlist Book Now Available!",
                    "Good news! '" + w.getBook().getTitle() + "' is now available for borrowing in your library.",
                    "CIRCULATION"
            );
            notificationRepository.save(n);
        }
    }
}
