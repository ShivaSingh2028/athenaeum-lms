package com.college.library.fake;

import com.college.library.entity.Notification;
import com.college.library.entity.User;
import com.college.library.repository.NotificationRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeNotificationRepository implements NotificationRepository {

    private final Map<Long, Notification> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<Notification> findByTenantIdAndUserOrderByCreatedAtDesc(String tenantId, User user) {
        return storage.values().stream()
                .filter(n -> Objects.equals(n.getTenantId(), tenantId) && n.getUser() != null && Objects.equals(n.getUser().getId(), user.getId()))
                .sorted(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public long countByTenantIdAndUserAndIsReadFalse(String tenantId, User user) {
        return storage.values().stream()
                .filter(n -> Objects.equals(n.getTenantId(), tenantId) && n.getUser() != null && Objects.equals(n.getUser().getId(), user.getId()) && !n.isRead())
                .count();
    }

    @Override
    public <S extends Notification> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<Notification> findById(Long id) { return Optional.ofNullable(storage.get(id)); }
    @Override public boolean existsById(Long id) { return storage.containsKey(id); }
    @Override public List<Notification> findAll() { return new ArrayList<>(storage.values()); }
    @Override public List<Notification> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
    @Override public long count() { return storage.size(); }
    @Override public void deleteById(Long id) { storage.remove(id); }
    @Override public void delete(Notification entity) { if (entity != null) storage.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends Notification> entities) {}
    @Override public void deleteAll() { storage.clear(); }
    @Override public void flush() {}
    @Override public <S extends Notification> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Notification> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Notification> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public Notification getOne(Long aLong) { return storage.get(aLong); }
    @Override public Notification getById(Long aLong) { return storage.get(aLong); }
    @Override public Notification getReferenceById(Long aLong) { return storage.get(aLong); }
    @Override public <S extends Notification> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Notification> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends Notification> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends Notification> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Notification> long count(Example<S> example) { return 0; }
    @Override public <S extends Notification> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Notification, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Notification> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<Notification> findAll(Sort sort) { return findAll(); }
    @Override public Page<Notification> findAll(Pageable pageable) { return Page.empty(); }
}
