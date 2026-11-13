package ru.nsu.resortbooking.adapter.web;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.resortbooking.application.BookingService;
import ru.nsu.resortbooking.application.BookingService.Download;
import ru.nsu.resortbooking.domain.Booking;
import ru.nsu.resortbooking.domain.Stay;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/staff")
public class StaffController {
    private final BookingService bookings;

    public StaffController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/requests")
    public List<BookingResponse> requests(@RequestParam(required = false) Booking.Status status) {
        return bookings.staffRequests(status).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @PostMapping("/requests/{id}/approve")
    public BookingResponse approve(@PathVariable long id) {
        return BookingResponse.from(bookings.review(id, true));
    }

    @PostMapping("/requests/{id}/reject")
    public BookingResponse reject(@PathVariable long id) {
        return BookingResponse.from(bookings.review(id, false));
    }

    @GetMapping("/sessions")
    public List<Stay> sessions() {
        return bookings.listStays();
    }

    @PostMapping("/sessions")
    public Stay createSession(@RequestBody SessionRequest request) {
        return bookings.createStay(request.startDate(), request.endDate(), request.capacity());
    }

    @GetMapping("/requests/{id}/documents")
    public List<DocumentResponse> documents(@PathVariable long id) {
        return bookings.attachments(id).stream()
                .map(document -> new DocumentResponse(document.id(), document.fileName()))
                .toList();
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        Download document = bookings.download(id);
        Resource resource = new InputStreamResource(document.content()::open);

        String disposition = ContentDisposition.attachment()
                .filename(document.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    public record SessionRequest(LocalDate startDate, LocalDate endDate, Integer capacity) {
    }

    public record DocumentResponse(long id, String fileName) {
    }
}
