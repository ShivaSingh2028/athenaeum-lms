package com.college.library.tenant;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;

    public TenantService(TenantRepository tenantRepository, TenantSettingsRepository tenantSettingsRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
    }

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public Optional<Tenant> findById(String id) {
        return tenantRepository.findById(id);
    }

    public Optional<Tenant> findBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain);
    }

    @Cacheable(value = "tenantSettings", key = "#tenantId")
    public TenantSettings getSettings(String tenantId) {
        return tenantSettingsRepository.findById(tenantId)
                .orElseGet(() -> {
                    TenantSettings defaults = new TenantSettings(tenantId, "Athenaeum Library");
                    return tenantSettingsRepository.save(defaults);
                });
    }

    @Transactional
    @CacheEvict(value = "tenantSettings", key = "#settings.tenantId")
    public TenantSettings updateSettings(TenantSettings settings) {
        settings.setUpdatedAt(OffsetDateTime.now());
        return tenantSettingsRepository.save(settings);
    }

    @Transactional
    public Tenant registerTenant(String id, String name, String subdomain, String contactEmail) {
        if (tenantRepository.existsById(id)) {
            throw new IllegalArgumentException("Tenant ID already exists: " + id);
        }
        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new IllegalArgumentException("Subdomain already in use: " + subdomain);
        }

        Tenant tenant = new Tenant(id, name, subdomain, contactEmail);
        tenantRepository.save(tenant);

        TenantSettings settings = new TenantSettings(id, name);
        tenantSettingsRepository.save(settings);

        return tenant;
    }
}
