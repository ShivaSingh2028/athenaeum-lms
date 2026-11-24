package com.college.library.controller;

import com.college.library.catalog.BulkCatalogService;
import com.college.library.catalog.BulkImportResult;
import com.college.library.entity.Book;
import com.college.library.entity.BookCopy;
import com.college.library.entity.CopyCondition;
import com.college.library.service.BookCopyService;
import com.college.library.service.BookService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final BulkCatalogService bulkCatalogService;

    public BookController(BookService bookService, BookCopyService bookCopyService, BulkCatalogService bulkCatalogService) {
        this.bookService = bookService;
        this.bookCopyService = bookCopyService;
        this.bulkCatalogService = bulkCatalogService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("books", bookService.search(q));
        model.addAttribute("query", q);
        return "books";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("book", new Book());
        return "book-form";
    }

    @PostMapping
    public String create(Book book) {
        bookService.addBook(book);
        return "redirect:/books";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.findById(id));
        return "book-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Book book) {
        bookService.updateBook(id, book);
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        bookService.deleteById(id);
        return "redirect:/books";
    }

    @GetMapping("/{id}/copies")
    public String viewCopies(@PathVariable Long id, Model model) {
        Book book = bookService.findById(id);
        List<BookCopy> copies = bookCopyService.getCopiesForBook(id);
        model.addAttribute("book", book);
        model.addAttribute("copies", copies);
        return "book-copies";
    }

    @PostMapping("/{id}/copies/add")
    public String addCopies(@PathVariable Long id, 
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(defaultValue = "General Rack") String shelfLocation,
                            @RequestParam(defaultValue = "GOOD") CopyCondition condition) {
        Book book = bookService.findById(id);
        bookCopyService.createCopies(book, quantity, shelfLocation, condition);
        return "redirect:/books/" + id + "/copies";
    }

    @PostMapping("/import-csv")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            @RequestParam(defaultValue = "false") boolean dryRun,
                            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid CSV file to upload.");
            return "redirect:/books";
        }

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            BulkImportResult result = bulkCatalogService.importCsv(reader, dryRun);
            if (result.getErrorRows() > 0) {
                redirectAttributes.addFlashAttribute("importWarning",
                        "Import finished with " + result.getSuccessfulRows() + " valid rows, but " +
                        result.getErrorRows() + " errors. First error: " + result.getErrors().get(0).getMessage());
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        (dryRun ? "[Dry Run Validation] " : "") + "Successfully processed " + result.getSuccessfulRows() +
                        " books (" + result.getCopiesCreated() + " copies created).");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "CSV Import failed: " + e.getMessage());
        }
        return "redirect:/books";
    }

    @GetMapping("/export-csv")
    public void exportCsv(HttpServletResponse response) {
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"catalog_export.csv\"");
            bulkCatalogService.exportCsv(response.getWriter());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
