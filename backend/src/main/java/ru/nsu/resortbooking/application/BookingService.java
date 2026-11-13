package ru.nsu.resortbooking.application;

import ru.nsu.resortbooking.application.port.BookingStore;
import ru.nsu.resortbooking.application.port.BookingStore.Attachment;
import ru.nsu.resortbooking.application.port.BookingStore.BookingView;
import ru.nsu.resortbooking.application.port.DocumentStorage;
import ru.nsu.resortbooking.application.port.UnitOfWork;
import ru.nsu.resortbooking.domain.Booking;
import ru.nsu.resortbooking.domain.Stay;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BookingService {
    private final BookingStore store;
    private final DocumentStorage files;
    private final UnitOfWork transactions;
    private final Clock clock;

    public BookingService(
            BookingStore store,
            DocumentStorage files,
            UnitOfWork transactions,
            Clock clock
    ) {
        this.store = store;
        this.files = files;
        this.transactions = transactions;
        this.clock = clock;
    }

    public List<Stay> listStays() {
        return store.listStays();
    }

    public Stay createStay(LocalDate startDate, LocalDate endDate, Integer capacity) {
        if (capacity == null) {
            throw new IllegalArgumentException("Capacity is required");
        }
        return transactions.execute(() -> store.saveStay(new Stay(0, startDate, endDate, capacity, 0)));
    }

    public BookingView submit(long studentId, long stayId, String comment, List<Upload> uploads) {
        if (uploads.isEmpty() || uploads.stream().anyMatch(Upload::empty)) {
            throw new IllegalArgumentException("At least one non-empty document is required");
        }
        if (comment != null && comment.length() > 2048) {
            throw new IllegalArgumentException("Comment must not exceed 2048 characters");
        }
        return transactions.execute(() -> {
            store.findStay(stayId).orElseThrow(() -> new NotFoundException("Session not found"));
            if (store.exists(studentId, stayId)) {
                throw new IllegalStateException("A request already exists for this session");
            }

            Booking booking = store.saveBooking(new Booking(
                    0,
                    studentId,
                    stayId,
                    Booking.Status.PENDING,
                    false,
                    comment,
                    LocalDateTime.now(clock)
            ));

            for (Upload upload : uploads) {
                String key = files.save(upload.content());
                transactions.onRollback(() -> files.delete(key));
                store.addAttachment(booking.id(), upload.fileName(), key);
            }

            return store.view(booking);
        });
    }

    public List<BookingView> studentRequests(long studentId) {
        return store.listForStudent(studentId);
    }

    public List<BookingView> staffRequests(Booking.Status status) {
        return store.listForStaff(status);
    }

    public BookingView review(long id, boolean approve) {
        return transactions.execute(() -> {
            Booking updated = requireLockedBooking(id).review(approve);
            if (approve) {
                Stay stay = store.lockStay(updated.sessionId())
                        .orElseThrow(() -> new NotFoundException("Session not found"));
                store.saveStay(stay.reserve());
            }

            return store.view(store.saveBooking(updated));
        });
    }

    public BookingView markPaid(long id, long studentId) {
        return transactions.execute(() -> {
            Booking booking = requireLockedBooking(id);
            if (booking.studentId() != studentId) {
                throw new NotFoundException("Request not found");
            }
            return store.view(store.saveBooking(booking.markPaid()));
        });
    }

    public List<Attachment> attachments(long bookingId) {
        if (!store.bookingExists(bookingId)) {
            throw new NotFoundException("Request not found");
        }
        return store.listAttachments(bookingId);
    }

    public Download download(long id) {
        Attachment attachment = store.findAttachment(id)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        return new Download(attachment.fileName(), files.read(attachment.storageKey()));
    }

    private Booking requireLockedBooking(long id) {
        return store.lockBooking(id).orElseThrow(() -> new NotFoundException("Request not found"));
    }

    public record Upload(String fileName, boolean empty, DocumentStorage.Content content) {
        public Upload {
            if (fileName == null || fileName.isBlank() || fileName.length() > 255
                    || fileName.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid document name");
            }
        }
    }

    public record Download(String fileName, DocumentStorage.Content content) {
    }
}
