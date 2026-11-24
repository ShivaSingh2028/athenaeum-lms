package com.college.library.finance;

import com.college.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<Fine> findByTenantIdAndUserOrderByCreatedAtDesc(String tenantId, User user);

    List<Fine> findByTenantIdAndStatus(String tenantId, FineStatus status);

    @Query("SELECT COALESCE(SUM(f.amount - f.paidAmount - f.waivedAmount), 0) FROM Fine f " +
           "WHERE f.tenantId = :tenantId AND f.user = :user AND f.status IN ('ACCRUED', 'PARTIALLY_PAID')")
    BigDecimal sumUnpaidFinesForUser(@Param("tenantId") String tenantId, @Param("user") User user);

    @Query("SELECT COALESCE(SUM(f.paidAmount), 0) FROM Fine f WHERE f.tenantId = :tenantId")
    BigDecimal sumTotalPaidFines(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(SUM(f.amount - f.paidAmount - f.waivedAmount), 0) FROM Fine f WHERE f.tenantId = :tenantId AND f.status IN ('ACCRUED', 'PARTIALLY_PAID')")
    BigDecimal sumTotalOutstandingFines(@Param("tenantId") String tenantId);
}
