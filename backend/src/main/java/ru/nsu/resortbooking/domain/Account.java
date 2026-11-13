package ru.nsu.resortbooking.domain;

import java.util.Locale;

public record Account(long id, String email, Role role) {
    public enum Role {
        STUDENT, STAFF
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
