package com.college.library.privacy;

import java.time.OffsetDateTime;
import java.util.List;

public class PatronDataExportDto {
    private Long patronId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String memberCode;
    private boolean readingHistoryPrivate;
    private OffsetDateTime exportedAt = OffsetDateTime.now();

    private List<LoanRecordDto> loans;
    private List<FineRecordDto> fines;
    private List<PaymentRecordDto> payments;

    public record LoanRecordDto(String bookTitle, String barcode, String issueDate, String dueDate, String returnDate, String status) {}
    public record FineRecordDto(String amount, String paidAmount, String status, String waiverReason) {}
    public record PaymentRecordDto(String amount, String paymentMethod, String status, String transactionRef, String paymentDate) {}

    public Long getPatronId() { return patronId; }
    public void setPatronId(Long patronId) { this.patronId = patronId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public boolean isReadingHistoryPrivate() { return readingHistoryPrivate; }
    public void setReadingHistoryPrivate(boolean readingHistoryPrivate) { this.readingHistoryPrivate = readingHistoryPrivate; }

    public OffsetDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(OffsetDateTime exportedAt) { this.exportedAt = exportedAt; }

    public List<LoanRecordDto> getLoans() { return loans; }
    public void setLoans(List<LoanRecordDto> loans) { this.loans = loans; }

    public List<FineRecordDto> getFines() { return fines; }
    public void setFines(List<FineRecordDto> fines) { this.fines = fines; }

    public List<PaymentRecordDto> getPayments() { return payments; }
    public void setPayments(List<PaymentRecordDto> payments) { this.payments = payments; }
}
