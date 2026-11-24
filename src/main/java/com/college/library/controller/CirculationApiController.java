package com.college.library.controller;

import com.college.library.entity.BookCopy;
import com.college.library.entity.Transaction;
import com.college.library.entity.User;
import com.college.library.service.BookCopyService;
import com.college.library.service.TransactionService;
import com.college.library.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/circulation")
public class CirculationApiController {

    private final TransactionService transactionService;
    private final BookCopyService bookCopyService;
    private final UserService userService;

    public CirculationApiController(TransactionService transactionService,
                                  BookCopyService bookCopyService,
                                  UserService userService) {
        this.transactionService = transactionService;
        this.bookCopyService = bookCopyService;
        this.userService = userService;
    }

    public record IssueRequest(String barcode, String memberCode) {}
    public record ReturnRequest(String barcode, String condition) {}

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN', 'LIBRARY_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> issueBook(@RequestBody IssueRequest req,
                                       @AuthenticationPrincipal UserDetails staffDetails) {
        try {
            User patron = userService.findAll().stream()
                    .filter(u -> req.memberCode().equalsIgnoreCase(u.getMemberCode()) || req.memberCode().equalsIgnoreCase(u.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Patron not found with identifier: " + req.memberCode()));

            BookCopy copy = bookCopyService.findByBarcode(req.barcode())
                    .orElseThrow(() -> new IllegalArgumentException("Book copy not found with barcode: " + req.barcode()));

            User staff = userService.findByEmail(staffDetails.getUsername()).orElseThrow();
            Transaction tx = transactionService.issueCopy(copy, patron, staff);

            return ResponseEntity.ok(Map.of(
                    "status", "ISSUED",
                    "transactionId", tx.getId(),
                    "barcode", req.barcode(),
                    "dueDate", tx.getDueDate().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN', 'LIBRARY_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> returnBook(@RequestBody ReturnRequest req) {
        try {
            BookCopy copy = bookCopyService.findByBarcode(req.barcode())
                    .orElseThrow(() -> new IllegalArgumentException("Book copy not found with barcode: " + req.barcode()));

            Transaction activeTx = transactionService.findAllIssued().stream()
                    .filter(t -> t.getBookCopy() != null && t.getBookCopy().getId().equals(copy.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No active loan found for copy: " + req.barcode()));

            Transaction tx = transactionService.returnBook(activeTx.getId());

            return ResponseEntity.ok(Map.of(
                    "status", "RETURNED",
                    "transactionId", tx.getId(),
                    "fineAssessed", tx.getFineAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/renew/{transactionId}")
    public ResponseEntity<?> renewLoan(@PathVariable Long transactionId) {
        try {
            Transaction existing = transactionService.findById(transactionId);
            Transaction tx = transactionService.renewLoan(transactionId, existing.getUser());
            return ResponseEntity.ok(Map.of(
                    "status", "RENEWED",
                    "transactionId", tx.getId(),
                    "newDueDate", tx.getDueDate().toString(),
                    "renewalCount", tx.getRenewalCount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
