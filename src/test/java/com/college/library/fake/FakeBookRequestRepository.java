package com.college.library.fake;

import com.college.library.catalog.BookRequest;
import com.college.library.catalog.BookRequestRepository;
import com.college.library.catalog.BookRequestStatus;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeBookRequestRepository implements BookRequestRepository {

    private final Map<Long, BookRequest> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<BookRequest> findByTenantIdOrderByCreatedAtDesc(String tenantId) {
        return storage.values().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId))
                .sorted(Comparator.comparing(BookRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<BookRequest> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, Long userId) {
        return storage.values().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId) && r.getUser() != null && Objects.equals(r.getUser().getId(), userId))
                .sorted(Comparator.comparing(BookRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<BookRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, BookRequestStatus status) {
        return storage.values().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId) && r.getStatus() == status)
                .sorted(Comparator.comparing(BookRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public <S extends BookRequest> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        } else {
            entity.preUpdate();
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<BookRequest> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override public boolean existsById(Long id) { return storage.containsKey(id); }
    @Override public List<BookRequest> findAll() { return new ArrayList<>(storage.values()); }
    @Override public List<BookRequest> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
    @Override public long count() { return storage.size(); }
    @Override public void deleteById(Long id) { storage.remove(id); }
    @Override public void delete(BookRequest entity) { if (entity != null) storage.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends BookRequest> entities) {}
    @Override public void deleteAll() { storage.clear(); }
    @Override public void flush() {}
    @Override public <S extends BookRequest> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends BookRequest> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<BookRequest> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public BookRequest getOne(Long aLong) { return storage.get(aLong); }
    @Override public BookRequest getById(Long aLong) { return storage.get(aLong); }
    @Override public BookRequest getReferenceById(Long aLong) { return storage.get(aLong); }
    @Override public <S extends BookRequest> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends BookRequest> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends BookRequest> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends BookRequest> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends BookRequest> long count(Example<S> example) { return 0; }
    @Override public <S extends BookRequest> boolean exists(Example<S> example) { return false; }
    @Override public <S extends BookRequest, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends BookRequest> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<BookRequest> findAll(Sort sort) { return findAll(); }
    @Override public Page<BookRequest> findAll(Pageable pageable) { return Page.empty(); }
}
