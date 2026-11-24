package com.college.library.barcode;

import com.college.library.entity.Book;
import com.college.library.entity.BookCopy;
import com.college.library.entity.User;
import com.college.library.service.BookCopyService;
import com.college.library.service.BookService;
import com.college.library.service.UserService;
import com.college.library.tenant.TenantContext;
import com.college.library.tenant.TenantService;
import com.college.library.tenant.TenantSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BarcodeController {

    private final BarcodeService barcodeService;
    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final UserService userService;
    private final TenantService tenantService;

    public BarcodeController(BarcodeService barcodeService,
                             BookService bookService,
                             BookCopyService bookCopyService,
                             UserService userService,
                             TenantService tenantService) {
        this.barcodeService = barcodeService;
        this.bookService = bookService;
        this.bookCopyService = bookCopyService;
        this.userService = userService;
        this.tenantService = tenantService;
    }

    @GetMapping("/barcode/{code}")
    @ResponseBody
    public ResponseEntity<byte[]> getBarcode(@PathVariable String code,
                                             @RequestParam(defaultValue = "260") int width,
                                             @RequestParam(defaultValue = "70") int height) {
        try {
            byte[] image = barcodeService.generateCode128(code, width, height);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(image);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/barcode/qr/{code}")
    @ResponseBody
    public ResponseEntity<byte[]> getQrCode(@PathVariable String code,
                                            @RequestParam(defaultValue = "200") int size) {
        try {
            byte[] image = barcodeService.generateQrCode(code, size, size);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(image);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping({"/barcodes/print", "/barcode/print"})
    public String printGeneralLabels(Model model) {
        List<BookCopy> copies = bookCopyService.findAll().stream().limit(50).toList();
        TenantSettings settings = tenantService.getSettings(TenantContext.getTenantId());
        model.addAttribute("copies", copies);
        model.addAttribute("settings", settings);
        return "print-labels";
    }

    @GetMapping("/books/{id}/copies/print-labels")
    public String printCopyLabels(@PathVariable Long id, Model model) {
        Book book = bookService.findById(id);
        List<BookCopy> copies = bookCopyService.getCopiesForBook(id);
        TenantSettings settings = tenantService.getSettings(TenantContext.getTenantId());

        model.addAttribute("book", book);
        model.addAttribute("copies", copies);
        model.addAttribute("settings", settings);
        return "print-labels";
    }

    @GetMapping("/admin/users/{id}/id-card")
    public String printMemberCard(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        TenantSettings settings = tenantService.getSettings(TenantContext.getTenantId());

        model.addAttribute("user", user);
        model.addAttribute("settings", settings);
        return "print-id-card";
    }
}
