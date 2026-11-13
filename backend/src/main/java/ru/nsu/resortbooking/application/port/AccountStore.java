package ru.nsu.resortbooking.application.port;

import ru.nsu.resortbooking.domain.Account;
import java.util.Optional;

public interface AccountStore {
    Optional<Credentials> findByEmail(String email);

    Account create(String email, Account.Role role, String passwordHash);

    record Credentials(Account account, String passwordHash) {
    }
}
