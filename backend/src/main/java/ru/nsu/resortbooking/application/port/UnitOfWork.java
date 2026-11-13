package ru.nsu.resortbooking.application.port;

import java.util.function.Supplier;

public interface UnitOfWork {
    <T> T execute(Supplier<T> action);

    void onRollback(Runnable action);
}
