package ru.nsu.resortbooking.adapter.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.nsu.resortbooking.application.NotFoundException;
import ru.nsu.resortbooking.application.port.DocumentStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class LocalDocumentStorage implements DocumentStorage {
    private final Path directory;

    public LocalDocumentStorage(@Value("${file.storage.location}") String location) throws IOException {
        directory = Files.createDirectories(Path.of(location).toAbsolutePath().normalize());
    }

    @Override
    public String save(Content content) {
        String key = UUID.randomUUID().toString();
        Path path = directory.resolve(key);
        try (InputStream input = content.open()) {
            Files.copy(input, path);
            return key;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw new UncheckedIOException("Could not store document", exception);
        }
    }

    @Override
    public Content read(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new NotFoundException("Document not found");
        }
        return () -> Files.newInputStream(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not remove document", exception);
        }
    }

    private Path resolve(String key) {
        Path path = directory.resolve(key).normalize();
        if (!directory.equals(path.getParent()) || key.contains("/") || key.contains("\\")) {
            throw new NotFoundException("Document not found");
        }
        return path;
    }
}
