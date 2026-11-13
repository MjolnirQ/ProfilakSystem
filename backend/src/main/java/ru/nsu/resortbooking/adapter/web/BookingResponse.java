package ru.nsu.resortbooking.adapter.web;

import ru.nsu.resortbooking.application.port.BookingStore.BookingView;
import ru.nsu.resortbooking.domain.Booking;
import ru.nsu.resortbooking.domain.Stay;

import java.time.LocalDateTime;

public record BookingResponse(
        long id,
        Student user,
        Stay session,
        Booking.Status status,
        boolean isPaid,
        String comment,
        LocalDateTime createdAt
) {
    static BookingResponse from(BookingView view) {
        Booking booking = view.booking();
        return new BookingResponse(
                booking.id(),
                new Student(view.studentEmail()),
                view.stay(),
                booking.status(),
                booking.isPaid(),
                booking.comment(),
                booking.createdAt()
        );
    }

    public record Student(String email) {
    }
}
