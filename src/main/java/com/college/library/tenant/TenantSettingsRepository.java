package com.college.library.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, String> {
}
