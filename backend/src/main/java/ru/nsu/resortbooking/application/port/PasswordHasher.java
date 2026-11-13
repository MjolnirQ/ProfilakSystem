package ru.nsu.resortbooking.application.port;

public interface PasswordHasher {
    String hash(String password);
}
