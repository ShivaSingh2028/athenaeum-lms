package com.college.library.fake;

import com.college.library.entity.User;
import com.college.library.payment.Payment;
import com.college.library.payment.PaymentRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakePaymentRepository implements PaymentRepository {

    private final Map<Long, Payment> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public Optional<Payment> findByIdempotencyKey(String key) {
        return db.values().stream().filter(p -> Objects.equals(p.getIdempotencyKey(), key)).findFirst();
    }

    @Override
    public boolean existsByIdempotencyKey(String key) {
        return findByIdempotencyKey(key).isPresent();
    }

    @Override
    public Optional<Payment> findByTransactionReference(String ref) {
        return db.values().stream().filter(p -> Objects.equals(p.getTransactionReference(), ref)).findFirst();
    }

    @Override
    public List<Payment> findByTenantIdOrderByPaymentDateDesc(String tenantId) {
        return db.values().stream()
                .filter(p -> Objects.equals(p.getTenantId(), tenantId))
                .sorted(Comparator.comparing(Payment::getPaymentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByTenantIdAndUserOrderByPaymentDateDesc(String tenantId, User user) {
        return db.values().stream()
                .filter(p -> Objects.equals(p.getTenantId(), tenantId) && p.getUser() != null && Objects.equals(p.getUser().getId(), user.getId()))
                .sorted(Comparator.comparing(Payment::getPaymentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public <S extends Payment> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<Payment> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<Payment> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<Payment> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(Payment entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends Payment> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends Payment> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Payment> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Payment> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public Payment getOne(Long id) { return db.get(id); }
    @Override public Payment getById(Long id) { return db.get(id); }
    @Override public Payment getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends Payment> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Payment> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends Payment> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends Payment> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Payment> long count(Example<S> example) { return 0; }
    @Override public <S extends Payment> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Payment, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Payment> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<Payment> findAll(Sort sort) { return findAll(); }
    @Override public Page<Payment> findAll(Pageable pageable) { return Page.empty(); }
}
