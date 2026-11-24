package com.college.library.catalog;

import com.college.library.entity.Notification;
import com.college.library.entity.User;
import com.college.library.repository.NotificationRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BookRequestService {

    private final BookRequestRepository bookRequestRepository;
    private final NotificationRepository notificationRepository;

    public BookRequestService(BookRequestRepository bookRequestRepository,
                              NotificationRepository notificationRepository) {
        this.bookRequestRepository = bookRequestRepository;
        this.notificationRepository = notificationRepository;
    }

    public List<BookRequest> getAllRequests() {
        return bookRequestRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId());
    }

    public List<BookRequest> getUserRequests(Long userId) {
        return bookRequestRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(TenantContext.getTenantId(), userId);
    }

    @Transactional
    public BookRequest submitRequest(User user, String title, String author, String isbn, String publisher, String reason) {
        BookRequest request = new BookRequest(user, title.trim(), author.trim(), isbn != null ? isbn.trim() : null, publisher, reason);
        return bookRequestRepository.save(request);
    }

    @Transactional
    public BookRequest reviewRequest(Long requestId, User reviewer, BookRequestStatus status, String adminNotes) {
        BookRequest request = bookRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Book request not found: " + requestId));

        if (request.getTenantId() != null && !request.getTenantId().equals(TenantContext.getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Book request belongs to another library tenant.");
        }
        if (!reviewer.getRole().isStaffOrAdmin() || (reviewer.getTenantId() != null && request.getTenantId() != null && !reviewer.getTenantId().equals(request.getTenantId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Only authorized staff within this library tenant may review purchase requests.");
        }

        request.setStatus(status);
        request.setAdminNotes(adminNotes);
        request.setReviewedBy(reviewer);
        request.setUpdatedAt(OffsetDateTime.now());
        BookRequest saved = bookRequestRepository.save(request);

        // Notify patron
        String statusLabel = status == BookRequestStatus.APPROVED ? "approved" :
                             status == BookRequestStatus.ORDERED ? "ordered" :
                             status == BookRequestStatus.ACQUIRED ? "acquired and available" : "declined";

        Notification notification = new Notification(
                request.getUser(),
                "Book Purchase Request Update",
                "Your request for '" + request.getTitle() + "' has been " + statusLabel + "." +
                (adminNotes != null && !adminNotes.isBlank() ? " Note: " + adminNotes : ""),
                "CIRCULATION"
        );
        notificationRepository.save(notification);

        return saved;
    }
}
