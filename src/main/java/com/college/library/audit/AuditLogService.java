package com.college.library.audit;

import com.college.library.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog logAction(Long actorId, String actorEmail, String action, String entityName,
                              String entityId, String beforeState, String afterState) {
        AuditLog log = new AuditLog(actorId, actorEmail, action, entityName, entityId, beforeState, afterState);
        return auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId(), PageRequest.of(0, limit));
    }

    public List<AuditLog> getEntityHistory(String entityName, String entityId) {
        return auditLogRepository.findByTenantIdAndEntityNameAndEntityIdOrderByCreatedAtDesc(
                TenantContext.getTenantId(), entityName, entityId
        );
    }
}
