package com.college.library.fake;

import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.repository.UserRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeUserRepository implements UserRepository {

    private final Map<Long, User> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public Optional<User> findByTenantIdAndEmail(String tenantId, String email) {
        return db.values().stream()
                .filter(u -> Objects.equals(u.getTenantId(), tenantId) && u.getEmail() != null && u.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    @Override
    public Optional<User> findByTenantIdAndMemberCode(String tenantId, String memberCode) {
        return db.values().stream()
                .filter(u -> Objects.equals(u.getTenantId(), tenantId) && Objects.equals(u.getMemberCode(), memberCode))
                .findFirst();
    }

    @Override
    public List<User> findByTenantId(String tenantId) {
        return db.values().stream().filter(u -> Objects.equals(u.getTenantId(), tenantId)).collect(Collectors.toList());
    }

    @Override
    public List<User> findByTenantIdAndRole(String tenantId, Role role) {
        return db.values().stream().filter(u -> Objects.equals(u.getTenantId(), tenantId) && u.getRole() == role).collect(Collectors.toList());
    }

    @Override
    public boolean existsByTenantIdAndEmail(String tenantId, String email) {
        return findByTenantIdAndEmail(tenantId, email).isPresent();
    }

    @Override
    public long countByTenantId(String tenantId) {
        return db.values().stream().filter(u -> Objects.equals(u.getTenantId(), tenantId)).count();
    }

    @Override
    public <S extends User> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        } else {
            entity.preUpdate();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<User> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<User> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<User> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(User entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends User> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<User> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public User getOne(Long id) { return db.get(id); }
    @Override public User getById(Long id) { return db.get(id); }
    @Override public User getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends User> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends User> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends User> long count(Example<S> example) { return 0; }
    @Override public <S extends User> boolean exists(Example<S> example) { return false; }
    @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends User> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<User> findAll(Sort sort) { return findAll(); }
    @Override public Page<User> findAll(Pageable pageable) { return Page.empty(); }
}
