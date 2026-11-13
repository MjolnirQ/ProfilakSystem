package ru.nsu.resortbooking.domain;

import java.time.LocalDateTime;

public record Booking(
        long id,
        long studentId,
        long sessionId,
        Status status,
        boolean isPaid,
        String comment,
        LocalDateTime createdAt
) {
    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    public Booking review(boolean approved) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Only pending requests can be reviewed");
        }

        return new Booking(
                id,
                studentId,
                sessionId,
                approved ? Status.APPROVED : Status.REJECTED,
                isPaid,
                comment,
                createdAt
        );
    }

    public Booking markPaid() {
        if (status != Status.APPROVED) {
            throw new IllegalStateException("Only approved requests can be marked as paid");
        }

        return new Booking(id, studentId, sessionId, status, true, comment, createdAt);
    }
}
