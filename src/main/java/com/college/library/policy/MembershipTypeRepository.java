package com.college.library.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTypeRepository extends JpaRepository<MembershipType, Long> {

    List<MembershipType> findByTenantIdAndIsActiveTrue(String tenantId);

    List<MembershipType> findByTenantId(String tenantId);

    Optional<MembershipType> findByTenantIdAndCode(String tenantId, String code);

    boolean existsByTenantIdAndCode(String tenantId, String code);
}
