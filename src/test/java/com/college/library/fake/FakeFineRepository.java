package com.college.library.fake;

import com.college.library.entity.User;
import com.college.library.finance.Fine;
import com.college.library.finance.FineRepository;
import com.college.library.finance.FineStatus;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeFineRepository implements FineRepository {

    private final Map<Long, Fine> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<Fine> findByTenantIdOrderByCreatedAtDesc(String tenantId) {
        return db.values().stream()
                .filter(f -> Objects.equals(f.getTenantId(), tenantId))
                .sorted(Comparator.comparing(Fine::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Fine> findByTenantIdAndUserOrderByCreatedAtDesc(String tenantId, User user) {
        return db.values().stream()
                .filter(f -> Objects.equals(f.getTenantId(), tenantId) && f.getUser() != null && Objects.equals(f.getUser().getId(), user.getId()))
                .sorted(Comparator.comparing(Fine::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Fine> findByTenantIdAndStatus(String tenantId, FineStatus status) {
        return db.values().stream()
                .filter(f -> Objects.equals(f.getTenantId(), tenantId) && f.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumUnpaidFinesForUser(String tenantId, User user) {
        return db.values().stream()
                .filter(f -> Objects.equals(f.getTenantId(), tenantId) &&
                             f.getUser() != null && Objects.equals(f.getUser().getId(), user.getId()) &&
                             (f.getStatus() == FineStatus.ACCRUED || f.getStatus() == FineStatus.PARTIALLY_PAID))
                .map(Fine::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumTotalPaidFines(String tenantId) {
        return db.values().stream()
                .filter(f -> Objects.equals(f.getTenantId(), tenantId))
                .map(Fine::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal sumTotalOutstandingFines(String tenantId) {
        return db.values().stream()
                .filter(f -> Objects.equals(f.getTenantId(), tenantId) &&
                             (f.getStatus() == FineStatus.ACCRUED || f.getStatus() == FineStatus.PARTIALLY_PAID))
                .map(Fine::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public <S extends Fine> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        } else {
            entity.preUpdate();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<Fine> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<Fine> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<Fine> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(Fine entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends Fine> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends Fine> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Fine> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Fine> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public Fine getOne(Long id) { return db.get(id); }
    @Override public Fine getById(Long id) { return db.get(id); }
    @Override public Fine getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends Fine> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Fine> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends Fine> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends Fine> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Fine> long count(Example<S> example) { return 0; }
    @Override public <S extends Fine> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Fine, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Fine> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<Fine> findAll(Sort sort) { return findAll(); }
    @Override public Page<Fine> findAll(Pageable pageable) { return Page.empty(); }
}
