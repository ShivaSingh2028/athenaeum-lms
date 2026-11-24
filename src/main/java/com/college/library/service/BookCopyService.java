package com.college.library.service;

import com.college.library.entity.Book;
import com.college.library.entity.BookCopy;
import com.college.library.entity.CopyCondition;
import com.college.library.entity.CopyStatus;
import com.college.library.repository.BookCopyRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookCopyService {

    private final BookCopyRepository copyRepository;

    public BookCopyService(BookCopyRepository copyRepository) {
        this.copyRepository = copyRepository;
    }

    public List<BookCopy> getCopiesForBook(Long bookId) {
        return copyRepository.findByBookIdAndTenantId(bookId, TenantContext.getTenantId());
    }

    public Optional<BookCopy> findByBarcode(String barcode) {
        return copyRepository.findByTenantIdAndBarcode(TenantContext.getTenantId(), barcode.trim());
    }

    public BookCopy getById(Long id) {
        return copyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book copy not found: " + id));
    }

    @Transactional
    public List<BookCopy> createCopies(Book book, int quantity, String shelfLocation, CopyCondition condition) {
        List<BookCopy> created = new ArrayList<>();
        String tenantId = book.getTenantId() != null ? book.getTenantId() : TenantContext.getTenantId();
        long currentCount = copyRepository.countByBookIdAndTenantId(book.getId(), tenantId);

        for (int i = 1; i <= quantity; i++) {
            long copyNum = currentCount + i;
            String barcode = String.format("BC-%s-%d-%03d", tenantId.toUpperCase(), book.getId(), copyNum);
            
            // Check for barcode collisions
            if (copyRepository.existsByTenantIdAndBarcode(tenantId, barcode)) {
                barcode = barcode + "-" + System.currentTimeMillis() % 1000;
            }

            BookCopy copy = new BookCopy(book, barcode, shelfLocation, condition);
            created.add(copyRepository.save(copy));
        }
        return created;
    }

    @Transactional
    public synchronized BookCopy markIssued(BookCopy copy) {
        BookCopy fresh = copyRepository.findById(copy.getId()).orElse(copy);
        if (fresh.getStatus() != CopyStatus.AVAILABLE) {
            throw new IllegalStateException("Copy with barcode " + fresh.getBarcode() + " is not AVAILABLE (current: " + fresh.getStatus() + ")");
        }
        fresh.setStatus(CopyStatus.ISSUED);
        return copyRepository.save(fresh);
    }

    @Transactional
    public BookCopy markReturned(BookCopy copy, CopyCondition returnedCondition) {
        copy.setStatus(CopyStatus.AVAILABLE);
        if (returnedCondition != null) {
            copy.setCondition(returnedCondition);
        }
        return copyRepository.save(copy);
    }

    @Transactional
    public BookCopy markHold(BookCopy copy) {
        copy.setStatus(CopyStatus.RESERVED_HOLD);
        return copyRepository.save(copy);
    }

    @Transactional
    public BookCopy markStatus(Long copyId, CopyStatus status, String notes) {
        BookCopy copy = getById(copyId);
        copy.setStatus(status);
        return copyRepository.save(copy);
    }

    public Optional<BookCopy> findFirstAvailableCopy(Book book) {
        List<BookCopy> available = copyRepository.findByBookIdAndStatusAndTenantId(
                book.getId(), CopyStatus.AVAILABLE, TenantContext.getTenantId());
        return available.isEmpty() ? Optional.empty() : Optional.of(available.get(0));
    }

    public long countAvailableCopies(Book book) {
        return copyRepository.countByBookIdAndStatusAndTenantId(book.getId(), CopyStatus.AVAILABLE, TenantContext.getTenantId());
    }

    public long countTotalCopies(Book book) {
        return copyRepository.countByBookIdAndTenantId(book.getId(), TenantContext.getTenantId());
    }
}
