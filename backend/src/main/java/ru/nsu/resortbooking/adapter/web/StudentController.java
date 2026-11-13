package ru.nsu.resortbooking.adapter.web;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.resortbooking.application.AccountService;
import ru.nsu.resortbooking.application.BookingService;
import ru.nsu.resortbooking.application.BookingService.Upload;
import ru.nsu.resortbooking.domain.Stay;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {
    private final BookingService bookings;
    private final AccountService accounts;

    public StudentController(BookingService bookings, AccountService accounts) {
        this.bookings = bookings;
        this.accounts = accounts;
    }

    @GetMapping("/sessions")
    public List<Stay> sessions() {
        return bookings.listStays();
    }

    @GetMapping("/requests")
    public List<BookingResponse> requests(Authentication authentication) {
        return bookings.studentRequests(accounts.studentId(authentication.getName())).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookingResponse submit(
            Authentication authentication,
            @RequestParam long sessionId,
            @RequestParam(required = false) String comment,
            @RequestParam MultipartFile[] files
    ) {
        List<Upload> uploads = Arrays.stream(files)
                .map(file -> new Upload(file.getOriginalFilename(), file.isEmpty(), file::getInputStream))
                .toList();

        return BookingResponse.from(bookings.submit(
                accounts.studentId(authentication.getName()),
                sessionId,
                comment,
                uploads
        ));
    }

    @PostMapping("/requests/{id}/pay")
    public BookingResponse pay(@PathVariable long id, Authentication authentication) {
        return BookingResponse.from(bookings.markPaid(id, accounts.studentId(authentication.getName())));
    }
}
