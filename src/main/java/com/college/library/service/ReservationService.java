package com.college.library.service;

import com.college.library.entity.*;
import com.college.library.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    @Value("${library.reservation.hold-days:2}")
    private int holdDays;

    // @Lazy breaks the circular dependency: TransactionService needs ReservationService,
    // and ReservationService needs NotificationService to send alerts.
    public ReservationService(ReservationRepository reservationRepository,
                               @Lazy NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.notificationService = notificationService;
    }

    public Reservation reserve(Book book, User user) {
        if (book.isAvailable()) {
            throw new IllegalStateException("\"" + book.getTitle() + "\" is currently available — "
                    + "you can issue it directly instead of reserving it.");
        }
        Reservation r = new Reservation();
        r.setBook(book);
        r.setUser(user);
        r.setReservationDate(LocalDate.now());
        r.setStatus(ReservationStatus.PENDING);
        return reservationRepository.save(r);
    }

    /** Called after a book is returned — notifies the longest-waiting reserver, if any. */
    public void fulfillNextReservation(Book book) {
        List<Reservation> queue = reservationRepository
                .findByBookIdAndStatusOrderByReservationDateAsc(book.getId(), ReservationStatus.PENDING);
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
        return reservationRepository.findByUserOrderByReservationDateDesc(user);
    }

    public List<Reservation> findPending() {
        return reservationRepository.findByStatus(ReservationStatus.PENDING);
    }

    public void cancel(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
    }
}
