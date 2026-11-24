package com.college.library.fake;

import com.college.library.entity.Reservation;
import com.college.library.entity.ReservationStatus;
import com.college.library.entity.User;
import com.college.library.repository.ReservationRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeReservationRepository implements ReservationRepository {

    private final Map<Long, Reservation> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<Reservation> findByTenantIdAndBookIdAndStatusOrderByReservationDateAsc(String tenantId, Long bookId, ReservationStatus status) {
        return db.values().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId) &&
                             r.getBook() != null && Objects.equals(r.getBook().getId(), bookId) &&
                             r.getStatus() == status)
                .sorted(Comparator.comparing(Reservation::getReservationDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByTenantIdAndUserOrderByReservationDateDesc(String tenantId, User user) {
        return db.values().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId) && r.getUser() != null && Objects.equals(r.getUser().getId(), user.getId()))
                .sorted(Comparator.comparing(Reservation::getReservationDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByTenantIdAndStatus(String tenantId, ReservationStatus status) {
        return db.values().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId) && r.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public <S extends Reservation> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        } else {
            entity.preUpdate();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<Reservation> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<Reservation> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<Reservation> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(Reservation entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends Reservation> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends Reservation> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Reservation> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Reservation> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public Reservation getOne(Long id) { return db.get(id); }
    @Override public Reservation getById(Long id) { return db.get(id); }
    @Override public Reservation getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends Reservation> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Reservation> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends Reservation> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends Reservation> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Reservation> long count(Example<S> example) { return 0; }
    @Override public <S extends Reservation> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Reservation, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Reservation> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<Reservation> findAll(Sort sort) { return findAll(); }
    @Override public Page<Reservation> findAll(Pageable pageable) { return Page.empty(); }
}
