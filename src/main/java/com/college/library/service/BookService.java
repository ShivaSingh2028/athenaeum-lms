package com.college.library.service;

import com.college.library.entity.Book;
import com.college.library.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
    }

    public List<Book> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrIsbnContainingIgnoreCase(
                query, query, query);
    }

    public Book addBook(Book book) {
        book.setAddedOn(LocalDate.now());
        book.setAvailableCopies(book.getTotalCopies());
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book updated) {
        Book existing = findById(id);
        int copiesDelta = updated.getTotalCopies() - existing.getTotalCopies();

        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setIsbn(updated.getIsbn());
        existing.setCategory(updated.getCategory());
        existing.setPublisher(updated.getPublisher());
        existing.setTotalCopies(updated.getTotalCopies());
        // adjust available copies by the same delta so currently-issued copies stay consistent
        existing.setAvailableCopies(Math.max(0, existing.getAvailableCopies() + copiesDelta));

        return bookRepository.save(existing);
    }

    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }

    void decrementAvailableCopies(Book book) {
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);
    }

    void incrementAvailableCopies(Book book) {
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        bookRepository.save(book);
    }
}
