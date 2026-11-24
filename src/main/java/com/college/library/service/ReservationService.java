package com.college.library.service;

import com.college.library.entity.*;
import com.college.library.repository.ReservationRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    @Value("${library.reservation.hold-days:2}")
    private int holdDays;

    public ReservationService(ReservationRepository reservationRepository,
                               @Lazy NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Reservation reserve(Book book, User user) {
        if (book.isAvailable()) {
            throw new IllegalStateException("\"" + book.getTitle() + "\" is currently available — "
                    + "you can issue it directly instead of reserving it.");
        }
        Reservation r = new Reservation();
        r.setTenantId(TenantContext.getTenantId());
        r.setBook(book);
        r.setUser(user);
        r.setReservationDate(LocalDate.now());
        r.setStatus(ReservationStatus.PENDING);
        return reservationRepository.save(r);
    }

    public boolean hasPendingReservation(Book book) {
        return !reservationRepository.findByTenantIdAndBookIdAndStatusOrderByReservationDateAsc(
                TenantContext.getTenantId(), book.getId(), ReservationStatus.PENDING).isEmpty();
    }

    /** Called after a book is returned — notifies the longest-waiting reserver, if any. */
    @Transactional
    public void fulfillNextReservation(Book book) {
        List<Reservation> queue = reservationRepository
                .findByTenantIdAndBookIdAndStatusOrderByReservationDateAsc(
                        TenantContext.getTenantId(), book.getId(), ReservationStatus.PENDING);
        if (!queue.isEmpty()) {
            Reservation next = queue.get(0);
            next.setStatus(ReservationStatus.FULFILLED);
            next.setExpiryDate(LocalDate.now().plusDays(holdDays));
            reservationRepository.save(next);
            notificationService.notify(next.getUser(),
                    "\"" + book.getTitle() + "\" is now available for you! Please collect it within "
                            + holdDays + " day(s).");
        }
    }

    public List<Reservation> findByUser(User user) {
        return reservationRepository.findByTenantIdAndUserOrderByReservationDateDesc(TenantContext.getTenantId(), user);
    }

    public List<Reservation> findPending() {
        return reservationRepository.findByTenantIdAndStatus(TenantContext.getTenantId(), ReservationStatus.PENDING);
    }

    @Transactional
    public void cancel(Long reservationId, User user) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (r.getTenantId() != null && !r.getTenantId().equals(TenantContext.getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Reservation belongs to another library tenant.");
        }

        if (user != null && !user.getRole().isStaffOrAdmin() && !r.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only cancel your own reservations.");
        }

        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
    }

    @Transactional
    public void cancel(Long reservationId) {
        cancel(reservationId, null);
    }
}
