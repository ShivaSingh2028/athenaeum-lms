package com.college.library.fake;

import com.college.library.entity.Book;
import com.college.library.repository.BookRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeBookRepository implements BookRepository {

    private final Map<Long, Book> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<Book> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getTenantId(), tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Book> findByIdAndTenantId(Long id, String tenantId) {
        Book b = storage.get(id);
        if (b != null && Objects.equals(b.getTenantId(), tenantId)) {
            return Optional.of(b);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Book> findByTenantIdAndIsbn(String tenantId, String isbn) {
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getTenantId(), tenantId) && Objects.equals(b.getIsbn(), isbn))
                .findFirst();
    }

    @Override
    public List<Book> searchBooks(String tenantId, String query) {
        String q = query.toLowerCase();
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getTenantId(), tenantId))
                .filter(b -> (b.getTitle() != null && b.getTitle().toLowerCase().contains(q)) ||
                             (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(q)) ||
                             (b.getIsbn() != null && b.getIsbn().toLowerCase().contains(q)) ||
                             (b.getCategory() != null && b.getCategory().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findDistinctCategories(String tenantId) {
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getTenantId(), tenantId))
                .map(Book::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<Book> findByTenantIdAndCategoryIgnoreCase(String tenantId, String category) {
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getTenantId(), tenantId))
                .filter(b -> b.getCategory() != null && b.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    @Override
    public List<Book> findTop12ByTenantIdOrderByCreatedAtDesc(String tenantId) {
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getTenantId(), tenantId))
                .sorted(Comparator.comparing(Book::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .collect(Collectors.toList());
    }

    @Override
    public long countByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(b -> Objects.equals(b.getTenantId(), tenantId))
                .count();
    }

    @Override
    public <S extends Book> S save(S entity) {
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
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return storage.containsKey(id);
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }

    @Override
    public void delete(Book entity) {
        if (entity != null && entity.getId() != null) {
            storage.remove(entity.getId());
        }
    }

    @Override public void flush() {}
    @Override public <S extends Book> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Book> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Book> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public Book getOne(Long aLong) { return storage.get(aLong); }
    @Override public Book getById(Long aLong) { return storage.get(aLong); }
    @Override public Book getReferenceById(Long aLong) { return storage.get(aLong); }
    @Override public <S extends Book> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Book> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends Book> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends Book> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Book> long count(Example<S> example) { return 0; }
    @Override public <S extends Book> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Book, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Book> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<Book> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends Book> entities) {}
    @Override public void deleteAll() { storage.clear(); }
    @Override public List<Book> findAll(Sort sort) { return findAll(); }
    @Override public Page<Book> findAll(Pageable pageable) { return Page.empty(); }
}
