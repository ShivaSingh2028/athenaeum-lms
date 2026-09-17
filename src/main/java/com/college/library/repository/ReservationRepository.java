package com.college.library.repository;

import com.college.library.entity.Reservation;
import com.college.library.entity.ReservationStatus;
import com.college.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserOrderByReservationDateDesc(User user);
    List<Reservation> findByBookIdAndStatusOrderByReservationDateAsc(Long bookId, ReservationStatus status);
    List<Reservation> findByStatus(ReservationStatus status);
}
