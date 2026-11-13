package ru.nsu.resortbooking.domain;

import java.time.LocalDate;

public record Stay(long id, LocalDate startDate, LocalDate endDate, int capacity, int bookedCount) {
    public Stay {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)
                || capacity < 1 || bookedCount < 0 || bookedCount > capacity) {
            throw new IllegalArgumentException("Invalid session dates or capacity");
        }
    }

    public Stay reserve() {
        if (bookedCount == capacity) {
            throw new IllegalStateException("Session is full");
        }
        return new Stay(id, startDate, endDate, capacity, bookedCount + 1);
    }
}
