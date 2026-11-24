package com.college.library.fake;

import com.college.library.audit.AuditLog;
import com.college.library.audit.AuditLogRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeAuditLogRepository implements AuditLogRepository {

    private final Map<Long, AuditLog> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<AuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable) {
        return db.values().stream()
                .filter(a -> Objects.equals(a.getTenantId(), tenantId))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByTenantIdAndEntityNameAndEntityIdOrderByCreatedAtDesc(String tenantId, String entityName, String entityId) {
        return db.values().stream()
                .filter(a -> Objects.equals(a.getTenantId(), tenantId) &&
                             Objects.equals(a.getEntityName(), entityName) &&
                             Objects.equals(a.getEntityId(), entityId))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public <S extends AuditLog> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<AuditLog> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<AuditLog> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<AuditLog> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(AuditLog entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends AuditLog> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends AuditLog> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends AuditLog> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<AuditLog> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public AuditLog getOne(Long id) { return db.get(id); }
    @Override public AuditLog getById(Long id) { return db.get(id); }
    @Override public AuditLog getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends AuditLog> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends AuditLog> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends AuditLog> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends AuditLog> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends AuditLog> long count(Example<S> example) { return 0; }
    @Override public <S extends AuditLog> boolean exists(Example<S> example) { return false; }
    @Override public <S extends AuditLog, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends AuditLog> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<AuditLog> findAll(Sort sort) { return findAll(); }
    @Override public Page<AuditLog> findAll(Pageable pageable) { return Page.empty(); }
}
