package ru.nsu.resortbooking.application.port;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorage {
    String save(Content content);

    Content read(String key);

    void delete(String key);

    @FunctionalInterface
    interface Content {
        InputStream open() throws IOException;
    }
}
