package com.college.library.service;

import com.college.library.entity.Book;
import com.college.library.entity.CopyCondition;
import com.college.library.repository.BookRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyService bookCopyService;

    public BookService(BookRepository bookRepository, @Lazy BookCopyService bookCopyService) {
        this.bookRepository = bookRepository;
        this.bookCopyService = bookCopyService;
    }

    public List<Book> findAll() {
        List<Book> books = bookRepository.findByTenantId(TenantContext.getTenantId());
        populateCopyCounts(books);
        return books;
    }

    public Book findById(Long id) {
        Book book = bookRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
        populateCopyCounts(List.of(book));
        return book;
    }

    public List<Book> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        List<Book> books = bookRepository.searchBooks(TenantContext.getTenantId(), query.trim());
        populateCopyCounts(books);
        return books;
    }

    @Transactional
    public Book addBook(Book book) {
        int initialCopies = book.getTotalCopies() > 0 ? book.getTotalCopies() : 1;
        book.setTenantId(TenantContext.getTenantId());
        Book savedBook = bookRepository.save(book);

        // Automatically create discrete physical copies
        bookCopyService.createCopies(savedBook, initialCopies, "General Rack", CopyCondition.GOOD);
        populateCopyCounts(List.of(savedBook));
        return savedBook;
    }

    @Transactional
    public Book updateBook(Long id, Book updated) {
        Book existing = findById(id);
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setIsbn(updated.getIsbn());
        existing.setCategory(updated.getCategory());
        existing.setPublisher(updated.getPublisher());
        existing.setPublicationYear(updated.getPublicationYear());
        existing.setEdition(updated.getEdition());
        existing.setSynopsis(updated.getSynopsis());
        existing.setCoverImageUrl(updated.getCoverImageUrl());
        existing.setLanguage(updated.getLanguage());

        Book saved = bookRepository.save(existing);
        populateCopyCounts(List.of(saved));
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        Book book = findById(id);
        bookRepository.delete(book);
    }

    public void populateCopyCounts(List<Book> books) {
        for (Book b : books) {
            long total = bookCopyService.countTotalCopies(b);
            long available = bookCopyService.countAvailableCopies(b);
            b.setTotalCopies((int) total);
            b.setAvailableCopies((int) available);
        }
    }
}
