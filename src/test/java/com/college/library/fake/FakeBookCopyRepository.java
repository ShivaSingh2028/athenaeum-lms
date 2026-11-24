package com.college.library.fake;

import com.college.library.entity.BookCopy;
import com.college.library.entity.CopyStatus;
import com.college.library.repository.BookCopyRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeBookCopyRepository implements BookCopyRepository {

    private final Map<Long, BookCopy> database = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<BookCopy> findByTenantIdAndBarcode(String tenantId, String barcode) {
        return database.values().stream()
                .filter(c -> Objects.equals(c.getTenantId(), tenantId) && Objects.equals(c.getBarcode(), barcode))
                .findFirst();
    }

    @Override
    public Optional<BookCopy> findByBarcode(String barcode) {
        return database.values().stream()
                .filter(c -> Objects.equals(c.getBarcode(), barcode))
                .findFirst();
    }

    @Override
    public List<BookCopy> findByBookIdAndTenantId(Long bookId, String tenantId) {
        return database.values().stream()
                .filter(c -> c.getBook() != null && Objects.equals(c.getBook().getId(), bookId)
                        && Objects.equals(c.getTenantId(), tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public List<BookCopy> findByBookIdAndStatusAndTenantId(Long bookId, CopyStatus status, String tenantId) {
        return database.values().stream()
                .filter(c -> c.getBook() != null && Objects.equals(c.getBook().getId(), bookId)
                        && c.getStatus() == status
                        && Objects.equals(c.getTenantId(), tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public long countByBookIdAndStatusAndTenantId(Long bookId, CopyStatus status, String tenantId) {
        return findByBookIdAndStatusAndTenantId(bookId, status, tenantId).size();
    }

    @Override
    public long countByBookIdAndTenantId(Long bookId, String tenantId) {
        return findByBookIdAndTenantId(bookId, tenantId).size();
    }

    @Override
    public long countByTenantIdAndStatus(String tenantId, CopyStatus status) {
        return database.values().stream()
                .filter(c -> Objects.equals(c.getTenantId(), tenantId) && c.getStatus() == status)
                .count();
    }

    @Override
    public List<BookCopy> findByTenantId(String tenantId) {
        return database.values().stream()
                .filter(c -> Objects.equals(c.getTenantId(), tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTenantIdAndBarcode(String tenantId, String barcode) {
        return findByTenantIdAndBarcode(tenantId, barcode).isPresent();
    }

    @Override
    public <S extends BookCopy> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        database.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<BookCopy> findById(Long id) {
        return Optional.ofNullable(database.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return database.containsKey(id);
    }

    @Override
    public List<BookCopy> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public long count() {
        return database.size();
    }

    @Override
    public void deleteById(Long id) {
        database.remove(id);
    }

    @Override
    public void delete(BookCopy entity) {
        if (entity != null && entity.getId() != null) {
            database.remove(entity.getId());
        }
    }

    // Default implementations for unused Spring Data JPA methods
    @Override public <S extends BookCopy> List<S> saveAll(Iterable<S> entities) { List<S> r = new ArrayList<>(); entities.forEach(e -> r.add(save(e))); return r; }
    @Override public List<BookCopy> findAllById(Iterable<Long> ids) { List<BookCopy> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(database::remove); }
    @Override public void deleteAll(Iterable<? extends BookCopy> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { database.clear(); }
    @Override public void flush() {}
    @Override public <S extends BookCopy> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends BookCopy> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<BookCopy> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { deleteAllById(ids); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public BookCopy getOne(Long id) { return database.get(id); }
    @Override public BookCopy getById(Long id) { return database.get(id); }
    @Override public BookCopy getReferenceById(Long id) { return database.get(id); }
    @Override public <S extends BookCopy> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends BookCopy> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends BookCopy> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends BookCopy> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends BookCopy> long count(Example<S> example) { return 0; }
    @Override public <S extends BookCopy> boolean exists(Example<S> example) { return false; }
    @Override public <S extends BookCopy, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public List<BookCopy> findAll(Sort sort) { return findAll(); }
    @Override public Page<BookCopy> findAll(Pageable pageable) { return Page.empty(); }
}
