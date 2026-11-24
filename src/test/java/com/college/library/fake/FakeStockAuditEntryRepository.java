package com.college.library.fake;

import com.college.library.audit.AuditEntryStatus;
import com.college.library.audit.StockAuditEntry;
import com.college.library.audit.StockAuditEntryRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeStockAuditEntryRepository implements StockAuditEntryRepository {

    private final Map<Long, StockAuditEntry> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<StockAuditEntry> findBySessionId(Long sessionId) {
        return db.values().stream()
                .filter(e -> e.getSession() != null && Objects.equals(e.getSession().getId(), sessionId))
                .collect(Collectors.toList());
    }

    @Override
    public List<StockAuditEntry> findBySessionIdAndStatus(Long sessionId, AuditEntryStatus status) {
        return db.values().stream()
                .filter(e -> e.getSession() != null && Objects.equals(e.getSession().getId(), sessionId) && e.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<StockAuditEntry> findBySessionIdAndBarcode(Long sessionId, String barcode) {
        return db.values().stream()
                .filter(e -> e.getSession() != null && Objects.equals(e.getSession().getId(), sessionId) && Objects.equals(e.getBarcode(), barcode))
                .findFirst();
    }

    @Override
    public boolean existsBySessionIdAndBarcode(Long sessionId, String barcode) {
        return findBySessionIdAndBarcode(sessionId, barcode).isPresent();
    }

    @Override
    public <S extends StockAuditEntry> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<StockAuditEntry> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<StockAuditEntry> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<StockAuditEntry> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(StockAuditEntry entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends StockAuditEntry> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends StockAuditEntry> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends StockAuditEntry> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<StockAuditEntry> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public StockAuditEntry getOne(Long id) { return db.get(id); }
    @Override public StockAuditEntry getById(Long id) { return db.get(id); }
    @Override public StockAuditEntry getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends StockAuditEntry> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends StockAuditEntry> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends StockAuditEntry> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends StockAuditEntry> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends StockAuditEntry> long count(Example<S> example) { return 0; }
    @Override public <S extends StockAuditEntry> boolean exists(Example<S> example) { return false; }
    @Override public <S extends StockAuditEntry, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends StockAuditEntry> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<StockAuditEntry> findAll(Sort sort) { return findAll(); }
    @Override public Page<StockAuditEntry> findAll(Pageable pageable) { return Page.empty(); }
}
