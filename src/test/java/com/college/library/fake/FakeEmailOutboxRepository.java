package com.college.library.fake;

import com.college.library.communication.EmailOutbox;
import com.college.library.communication.EmailOutboxRepository;
import com.college.library.communication.EmailStatus;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeEmailOutboxRepository implements EmailOutboxRepository {

    private final Map<Long, EmailOutbox> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<EmailOutbox> findPendingEmails(EmailStatus status, Pageable pageable) {
        return storage.values().stream()
                .filter(e -> e.getStatus() == status && e.getRetryCount() < e.getMaxRetries())
                .sorted(Comparator.comparing(EmailOutbox::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailOutbox> findByTenantIdOrderByCreatedAtDesc(String tenantId) {
        return storage.values().stream()
                .filter(e -> Objects.equals(e.getTenantId(), tenantId))
                .sorted(Comparator.comparing(EmailOutbox::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public <S extends EmailOutbox> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<EmailOutbox> findById(Long id) { return Optional.ofNullable(storage.get(id)); }
    @Override public boolean existsById(Long id) { return storage.containsKey(id); }
    @Override public List<EmailOutbox> findAll() { return new ArrayList<>(storage.values()); }
    @Override public List<EmailOutbox> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
    @Override public long count() { return storage.size(); }
    @Override public void deleteById(Long id) { storage.remove(id); }
    @Override public void delete(EmailOutbox entity) { if (entity != null) storage.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> longs) {}
    @Override public void deleteAll(Iterable<? extends EmailOutbox> entities) {}
    @Override public void deleteAll() { storage.clear(); }
    @Override public void flush() {}
    @Override public <S extends EmailOutbox> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends EmailOutbox> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<EmailOutbox> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
    @Override public void deleteAllInBatch() {}
    @Override public EmailOutbox getOne(Long aLong) { return storage.get(aLong); }
    @Override public EmailOutbox getById(Long aLong) { return storage.get(aLong); }
    @Override public EmailOutbox getReferenceById(Long aLong) { return storage.get(aLong); }
    @Override public <S extends EmailOutbox> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends EmailOutbox> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends EmailOutbox> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends EmailOutbox> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends EmailOutbox> long count(Example<S> example) { return 0; }
    @Override public <S extends EmailOutbox> boolean exists(Example<S> example) { return false; }
    @Override public <S extends EmailOutbox, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends EmailOutbox> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<EmailOutbox> findAll(Sort sort) { return findAll(); }
    @Override public Page<EmailOutbox> findAll(Pageable pageable) { return Page.empty(); }
}
