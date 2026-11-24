package com.college.library.fake;

import com.college.library.policy.MembershipType;
import com.college.library.policy.MembershipTypeRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeMembershipTypeRepository implements MembershipTypeRepository {

    private final Map<Long, MembershipType> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<MembershipType> findByTenantIdAndIsActiveTrue(String tenantId) {
        return db.values().stream()
                .filter(m -> Objects.equals(m.getTenantId(), tenantId) && m.isActive())
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipType> findByTenantId(String tenantId) {
        return db.values().stream().filter(m -> Objects.equals(m.getTenantId(), tenantId)).collect(Collectors.toList());
    }

    @Override
    public Optional<MembershipType> findByTenantIdAndCode(String tenantId, String code) {
        return db.values().stream()
                .filter(m -> Objects.equals(m.getTenantId(), tenantId) && Objects.equals(m.getCode(), code))
                .findFirst();
    }

    @Override
    public boolean existsByTenantIdAndCode(String tenantId, String code) {
        return findByTenantIdAndCode(tenantId, code).isPresent();
    }

    @Override
    public <S extends MembershipType> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<MembershipType> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<MembershipType> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<MembershipType> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(MembershipType entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends MembershipType> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends MembershipType> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends MembershipType> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<MembershipType> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public MembershipType getOne(Long id) { return db.get(id); }
    @Override public MembershipType getById(Long id) { return db.get(id); }
    @Override public MembershipType getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends MembershipType> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends MembershipType> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends MembershipType> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends MembershipType> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends MembershipType> long count(Example<S> example) { return 0; }
    @Override public <S extends MembershipType> boolean exists(Example<S> example) { return false; }
    @Override public <S extends MembershipType, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends MembershipType> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<MembershipType> findAll(Sort sort) { return findAll(); }
    @Override public Page<MembershipType> findAll(Pageable pageable) { return Page.empty(); }
}
