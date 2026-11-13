package ru.nsu.resortbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.nsu.resortbooking.application.AccountService;
import ru.nsu.resortbooking.application.BookingService;
import ru.nsu.resortbooking.application.port.AccountStore;
import ru.nsu.resortbooking.application.port.BookingStore;
import ru.nsu.resortbooking.application.port.DocumentStorage;
import ru.nsu.resortbooking.application.port.PasswordHasher;
import ru.nsu.resortbooking.application.port.UnitOfWork;

import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean
    AccountService accountService(AccountStore accounts, PasswordHasher passwords) {
        return new AccountService(accounts, passwords);
    }

    @Bean
    BookingService bookingService(BookingStore store, DocumentStorage files, UnitOfWork transactions) {
        return new BookingService(store, files, transactions, Clock.systemDefaultZone());
    }

    @Bean
    ApplicationRunner initialStaff(
            AccountService accounts,
            @Value("${PROFILAK_STAFF_EMAIL:}") String email,
            @Value("${PROFILAK_STAFF_PASSWORD:}") String password
    ) {
        return args -> accounts.initializeStaff(email, password);
    }
}
