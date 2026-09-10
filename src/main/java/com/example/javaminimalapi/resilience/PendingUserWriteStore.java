package com.example.javaminimalapi.resilience;

import com.example.javaminimalapi.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

// Files on disk, not memory, so queued writes survive an app restart or crash.
@Component
public class PendingUserWriteStore {

    private final ObjectMapper objectMapper;
    private final Path pendingDir;
    private final Path failedDir;

    public PendingUserWriteStore(
            ObjectMapper objectMapper,
            @Value("${app.resilience.pending-writes-dir:./data/pending-users}") String pendingWritesDir) throws IOException {
        this.objectMapper = objectMapper;
        this.pendingDir = Paths.get(pendingWritesDir);
        this.failedDir = pendingDir.resolve("failed");
        Files.createDirectories(pendingDir);
        Files.createDirectories(failedDir);
    }

    public void enqueue(UserDto payload) throws IOException {
        String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + ".json";
        Path target = pendingDir.resolve(fileName);
        Path tmp = pendingDir.resolve(fileName + ".tmp");
        objectMapper.writeValue(tmp.toFile(), payload);
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
    }

    public List<Path> listPending() throws IOException {
        try (Stream<Path> files = Files.list(pendingDir)) {
            return files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    public UserDto read(Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), UserDto.class);
    }

    public void markProcessed(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    public void markFailed(Path path) throws IOException {
        Files.move(path, failedDir.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }
}
