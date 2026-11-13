package ru.nsu.resortbooking.application.port;

import ru.nsu.resortbooking.domain.Booking;
import ru.nsu.resortbooking.domain.Stay;

import java.util.List;
import java.util.Optional;

public interface BookingStore {
    List<Stay> listStays();
    Optional<Stay> findStay(long id);
    Optional<Stay> lockStay(long id);
    Stay saveStay(Stay stay);

    boolean exists(long studentId, long stayId);
    Optional<Booking> lockBooking(long id);
    Booking saveBooking(Booking booking);
    List<BookingView> listForStudent(long studentId);
    List<BookingView> listForStaff(Booking.Status status);
    BookingView view(Booking booking);

    boolean bookingExists(long bookingId);
    List<Attachment> listAttachments(long bookingId);
    Optional<Attachment> findAttachment(long id);
    void addAttachment(long bookingId, String name, String key);

    record BookingView(Booking booking, String studentEmail, Stay stay) {
    }

    record Attachment(long id, String fileName, String storageKey) {
    }
}
