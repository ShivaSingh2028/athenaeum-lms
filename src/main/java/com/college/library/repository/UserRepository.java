package com.college.library.repository;

import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.tenant.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTenantIdAndEmail(String tenantId, String email);

    Optional<User> findByTenantIdAndMemberCode(String tenantId, String memberCode);

    List<User> findByTenantId(String tenantId);

    List<User> findByTenantIdAndRole(String tenantId, Role role);

    boolean existsByTenantIdAndEmail(String tenantId, String email);

    default Optional<User> findByEmail(String email) {
        return findByTenantIdAndEmail(TenantContext.getTenantId(), email);
    }

    default boolean existsByEmail(String email) {
        return existsByTenantIdAndEmail(TenantContext.getTenantId(), email);
    }

    long countByTenantId(String tenantId);
}
