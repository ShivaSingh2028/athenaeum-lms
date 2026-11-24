package com.college.library.repository;

import com.college.library.entity.Notification;
import com.college.library.entity.User;
import com.college.library.tenant.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTenantIdAndUserOrderByCreatedAtDesc(String tenantId, User user);

    long countByTenantIdAndUserAndIsReadFalse(String tenantId, User user);

    default List<Notification> findByUserOrderByCreatedAtDesc(User user) {
        return findByTenantIdAndUserOrderByCreatedAtDesc(TenantContext.getTenantId(), user);
    }

    default long countByUserAndIsReadFalse(User user) {
        return countByTenantIdAndUserAndIsReadFalse(TenantContext.getTenantId(), user);
    }
}
