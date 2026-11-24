package com.college.library.fake;

import com.college.library.entity.Transaction;
import com.college.library.entity.TransactionStatus;
import com.college.library.entity.User;
import com.college.library.repository.TransactionRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> db = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public List<Transaction> findByTenantIdOrderByIssueDateDesc(String tenantId) {
        return db.values().stream()
                .filter(t -> Objects.equals(t.getTenantId(), tenantId))
                .sorted(Comparator.comparing(Transaction::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByTenantIdAndUserOrderByIssueDateDesc(String tenantId, User user) {
        return db.values().stream()
                .filter(t -> Objects.equals(t.getTenantId(), tenantId) && t.getUser() != null && Objects.equals(t.getUser().getId(), user.getId()))
                .sorted(Comparator.comparing(Transaction::getIssueDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByTenantIdAndStatus(String tenantId, TransactionStatus status) {
        return db.values().stream()
                .filter(t -> Objects.equals(t.getTenantId(), tenantId) && t.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByTenantIdAndStatusAndDueDateBefore(String tenantId, TransactionStatus status, LocalDate date) {
        return db.values().stream()
                .filter(t -> Objects.equals(t.getTenantId(), tenantId) && t.getStatus() == status && t.getDueDate() != null && t.getDueDate().isBefore(date))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByTenantIdAndBookIdAndStatus(String tenantId, Long bookId, TransactionStatus status) {
        return db.values().stream()
                .filter(t -> Objects.equals(t.getTenantId(), tenantId) && t.getStatus() == status &&
                             ((t.getBook() != null && Objects.equals(t.getBook().getId(), bookId)) ||
                              (t.getBookCopy() != null && t.getBookCopy().getBook() != null && Objects.equals(t.getBookCopy().getBook().getId(), bookId))))
                .collect(Collectors.toList());
    }

    @Override
    public long countByTenantIdAndStatus(String tenantId, TransactionStatus status) {
        return db.values().stream()
                .filter(t -> Objects.equals(t.getTenantId(), tenantId) && t.getStatus() == status)
                .count();
    }

    @Override
    public <S extends Transaction> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGen.getAndIncrement());
            entity.prePersist();
        } else {
            entity.preUpdate();
        }
        db.put(entity.getId(), entity);
        return entity;
    }

    @Override public Optional<Transaction> findById(Long id) { return Optional.ofNullable(db.get(id)); }
    @Override public boolean existsById(Long id) { return db.containsKey(id); }
    @Override public List<Transaction> findAll() { return new ArrayList<>(db.values()); }
    @Override public List<Transaction> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
    @Override public long count() { return db.size(); }
    @Override public void deleteById(Long id) { db.remove(id); }
    @Override public void delete(Transaction entity) { if (entity != null) db.remove(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
    @Override public void deleteAll(Iterable<? extends Transaction> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { db.clear(); }
    @Override public void flush() {}
    @Override public <S extends Transaction> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Transaction> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Transaction> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public Transaction getOne(Long id) { return db.get(id); }
    @Override public Transaction getById(Long id) { return db.get(id); }
    @Override public Transaction getReferenceById(Long id) { return db.get(id); }
    @Override public <S extends Transaction> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Transaction> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
    @Override public <S extends Transaction> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
    @Override public <S extends Transaction> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Transaction> long count(Example<S> example) { return 0; }
    @Override public <S extends Transaction> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Transaction, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public <S extends Transaction> List<S> saveAll(Iterable<S> entities) {
        List<S> list = new ArrayList<>();
        for (S e : entities) list.add(save(e));
        return list;
    }
    @Override public List<Transaction> findAll(Sort sort) { return findAll(); }
    @Override public Page<Transaction> findAll(Pageable pageable) { return Page.empty(); }
}
