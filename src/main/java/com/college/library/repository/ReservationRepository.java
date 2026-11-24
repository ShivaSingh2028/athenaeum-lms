package com.college.library.repository;

import com.college.library.entity.Reservation;
import com.college.library.entity.ReservationStatus;
import com.college.library.entity.User;
import com.college.library.tenant.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByTenantIdAndBookIdAndStatusOrderByReservationDateAsc(String tenantId, Long bookId, ReservationStatus status);

    List<Reservation> findByTenantIdAndUserOrderByReservationDateDesc(String tenantId, User user);

    List<Reservation> findByTenantIdAndStatus(String tenantId, ReservationStatus status);

    default List<Reservation> findByBookIdAndStatusOrderByReservationDateAsc(Long bookId, ReservationStatus status) {
        return findByTenantIdAndBookIdAndStatusOrderByReservationDateAsc(TenantContext.getTenantId(), bookId, status);
    }

    default List<Reservation> findByUserOrderByReservationDateDesc(User user) {
        return findByTenantIdAndUserOrderByReservationDateDesc(TenantContext.getTenantId(), user);
    }

    default List<Reservation> findByStatus(ReservationStatus status) {
        return findByTenantIdAndStatus(TenantContext.getTenantId(), status);
    }
}
