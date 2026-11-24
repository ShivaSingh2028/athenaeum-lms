package com.college.library.fake;

import com.college.library.audit.StockAuditSession;
import com.college.library.audit.StockAuditSessionRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeStockAuditSessionRepository implements StockAuditSessionRepository {

    private final Map<Long, StockAuditSession> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<StockAuditSession> findByTenantIdOrderByStartedAtDesc(String tenantId) {
        return db.values().stream()
                .filter(s -> Objects.equals(s.getTenantId(), tenantId))
                .sorted(Comparator.comparing(StockAuditSession::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<StockAuditSession> findByIdAndTenantId(Long id, String tenantId) {
        StockAuditSession s = db.get(id);
        if (s != null && Objects.equals(s.getTenantId(), tenantId)) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    @Override
    public <S extends StockAuditSession> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<StockAuditSession> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<StockAuditSession> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<StockAuditSession> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(StockAuditSession entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends StockAuditSession> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends StockAuditSession> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends StockAuditSession> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<StockAuditSession> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public StockAuditSession getOne(Long id) { return db.get(id); }
    @Override public StockAuditSession getById(Long id) { return db.get(id); }
    @Override public StockAuditSession getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends StockAuditSession> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends StockAuditSession> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends StockAuditSession> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends StockAuditSession> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends StockAuditSession> long count(Example<S> example) { return 0; }
    @Override public <S extends StockAuditSession> boolean exists(Example<S> example) { return false; }
    @Override public <S extends StockAuditSession, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends StockAuditSession> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<StockAuditSession> findAll(Sort sort) { return findAll(); }
    @Override public Page<StockAuditSession> findAll(Pageable pageable) { return Page.empty(); }
}
