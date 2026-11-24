package com.college.library.fake;

import com.college.library.catalog.Wishlist;
import com.college.library.catalog.WishlistRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeWishlistRepository implements WishlistRepository {

    private final Map<Long, Wishlist> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<Wishlist> findByTenantIdAndUserId(String tenantId, Long userId) {
        return storage.values().stream()
                .filter(w -> Objects.equals(w.getTenantId(), tenantId) && w.getUser() != null && Objects.equals(w.getUser().getId(), userId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Wishlist> findByTenantIdAndUserIdAndBookId(String tenantId, Long userId, Long bookId) {
        return storage.values().stream()
                .filter(w -> Objects.equals(w.getTenantId(), tenantId) &&
                             w.getUser() != null && Objects.equals(w.getUser().getId(), userId) &&
                             w.getBook() != null && Objects.equals(w.getBook().getId(), bookId))
                .findFirst();
    }

    @Override
    public boolean existsByTenantIdAndUserIdAndBookId(String tenantId, Long userId, Long bookId) {
        return findByTenantIdAndUserIdAndBookId(tenantId, userId, bookId).isPresent();
    }

    @Override
    public void deleteByTenantIdAndUserIdAndBookId(String tenantId, Long userId, Long bookId) {
        findByTenantIdAndUserIdAndBookId(tenantId, userId, bookId).ifPresent(w -> storage.remove(w.getId()));
    }

    @Override
    public List<Wishlist> findByTenantIdAndBookIdAndNotifyOnAvailableTrue(String tenantId, Long bookId) {
        return storage.values().stream()
                .filter(w -> Objects.equals(w.getTenantId(), tenantId) &&
                             w.getBook() != null && Objects.equals(w.getBook().getId(), bookId) &&
                             w.isNotifyOnAvailable())
                .collect(Collectors.toList());
    }

    @Override
    public <S extends Wishlist> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Wishlist> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override public boolean existsById(Long id) { return storage.containsKey(id); }
    @Override public List<Wishlist> findAll() { return new ArrayList<>(storage.values()); }
    @Override public List<Wishlist> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
    @Override public long count() { return storage.size(); }
    @Override public void deleteById(Long id) { storage.remove(id); }
    @Override public void delete(Wishlist entity) { if (entity != null) storage.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends Wishlist> entities) {}
    @Override public void deleteAll() { storage.clear(); }
    @Override public void flush() {}
    @Override public <S extends Wishlist> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Wishlist> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Wishlist> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public Wishlist getOne(Long aLong) { return storage.get(aLong); }
    @Override public Wishlist getById(Long aLong) { return storage.get(aLong); }
    @Override public Wishlist getReferenceById(Long aLong) { return storage.get(aLong); }
    @Override public <S extends Wishlist> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Wishlist> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends Wishlist> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends Wishlist> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Wishlist> long count(Example<S> example) { return 0; }
    @Override public <S extends Wishlist> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Wishlist, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Wishlist> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<Wishlist> findAll(Sort sort) { return findAll(); }
    @Override public Page<Wishlist> findAll(Pageable pageable) { return Page.empty(); }
}
