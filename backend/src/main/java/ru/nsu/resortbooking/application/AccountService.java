package ru.nsu.resortbooking.application;

import ru.nsu.resortbooking.application.port.AccountStore;
import ru.nsu.resortbooking.application.port.PasswordHasher;
import ru.nsu.resortbooking.domain.Account;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AccountService {
    private final AccountStore accounts;
    private final PasswordHasher passwords;

    public AccountService(AccountStore accounts, PasswordHasher passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    public Account registerStudent(String email, String password) {
        String login = Account.normalizeEmail(email);
        if (!login.matches("[^@\\s]+@g\\.nsu\\.ru")) {
            throw new IllegalArgumentException("Student email must end with @g.nsu.ru");
        }
        if (accounts.findByEmail(login).isPresent()) {
            throw new IllegalStateException("Email already in use");
        }

        return create(login, password, Account.Role.STUDENT);
    }

    public void initializeStaff(String email, String password) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        String login = Account.normalizeEmail(email);
        Optional<AccountStore.Credentials> existing = accounts.findByEmail(login);
        if (existing.isPresent()) {
            if (existing.get().account().role() != Account.Role.STAFF) {
                throw new IllegalStateException("Configured staff email is already used by a student");
            }
            return;
        }
        create(login, password, Account.Role.STAFF);
    }

    public long studentId(String email) {
        return accounts.findByEmail(email)
                .map(AccountStore.Credentials::account)
                .filter(account -> account.role() == Account.Role.STUDENT)
                .orElseThrow(() -> new NotFoundException("Student account not found"))
                .id();
    }

    private Account create(String email, String password, Account.Role role) {
        if (email.length() > 255 || password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Email and a valid password are required");
        }
        return accounts.create(email, role, passwords.hash(password));
    }
}
