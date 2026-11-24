package com.college.library.entity;

public enum Role {
    SUPER_ADMIN,
    LIBRARY_ADMIN,
    LIBRARIAN,
    MEMBER,

    // Aliases for backward compatibility with initial student prototypes
    ADMIN,
    STUDENT;

    public boolean isStaffOrAdmin() {
        return this == SUPER_ADMIN || this == LIBRARY_ADMIN || this == ADMIN || this == LIBRARIAN;
    }

    public boolean isPlatformOwner() {
        return this == SUPER_ADMIN;
    }
}
