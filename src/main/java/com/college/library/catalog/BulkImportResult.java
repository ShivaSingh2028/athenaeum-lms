package com.college.library.catalog;

import java.util.ArrayList;
import java.util.List;

public class BulkImportResult {
    private boolean dryRun;
    private int totalRows;
    private int successfulRows;
    private int errorRows;
    private int booksCreated;
    private int copiesCreated;
    private List<RowError> errors = new ArrayList<>();

    public static class RowError {
        private int rowNumber;
        private String isbn;
        private String message;

        public RowError(int rowNumber, String isbn, String message) {
            this.rowNumber = rowNumber;
            this.isbn = isbn;
            this.message = message;
        }

        public int getRowNumber() { return rowNumber; }
        public String getIsbn() { return isbn; }
        public String getMessage() { return message; }
    }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getSuccessfulRows() { return successfulRows; }
    public void setSuccessfulRows(int successfulRows) { this.successfulRows = successfulRows; }

    public int getErrorRows() { return errorRows; }
    public void setErrorRows(int errorRows) { this.errorRows = errorRows; }

    public int getBooksCreated() { return booksCreated; }
    public void setBooksCreated(int booksCreated) { this.booksCreated = booksCreated; }

    public int getCopiesCreated() { return copiesCreated; }
    public void setCopiesCreated(int copiesCreated) { this.copiesCreated = copiesCreated; }

    public List<RowError> getErrors() { return errors; }
    public void setErrors(List<RowError> errors) { this.errors = errors; }

    public void addError(int rowNumber, String isbn, String message) {
        this.errors.add(new RowError(rowNumber, isbn, message));
        this.errorRows++;
    }
}
