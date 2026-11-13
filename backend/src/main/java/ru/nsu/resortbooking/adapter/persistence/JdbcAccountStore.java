package ru.nsu.resortbooking.adapter.persistence;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.nsu.resortbooking.application.port.AccountStore;
import ru.nsu.resortbooking.domain.Account;

import java.util.Optional;

@Repository
public class JdbcAccountStore implements AccountStore {
    private final JdbcClient jdbc;

    public JdbcAccountStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Credentials> findByEmail(String email) {
        return jdbc.sql("select id, email, role, password_hash from users where email = :email")
                .param("email", email)
                .query((rs, row) -> new Credentials(
                        new Account(rs.getLong("id"), rs.getString("email"),
                                Account.Role.valueOf(rs.getString("role"))),
                        rs.getString("password_hash")))
                .optional();
    }

    @Override
    public Account create(String email, Account.Role role, String passwordHash) {
        try {
            long id = jdbc.sql("""
                    insert into users(email, role, password_hash)
                    values (:email, :role, :hash) returning id
                    """)
                    .param("email", email)
                    .param("role", role.name())
                    .param("hash", passwordHash)
                    .query(Long.class)
                    .single();
            return new Account(id, email, role);
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException("Email already in use", exception);
        }
    }
}
