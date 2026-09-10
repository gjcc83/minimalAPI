package com.example.javaminimalapi.resilience;

import com.example.javaminimalapi.dto.UserDto;
import com.example.javaminimalapi.health.DatabaseHealthMonitor;
import com.example.javaminimalapi.repository.UserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
public class PendingUserWriteFlusher {

    private final PendingUserWriteStore store;
    private final UserRepository userRepository;
    private final DatabaseHealthMonitor databaseHealthMonitor;

    public PendingUserWriteFlusher(
            PendingUserWriteStore store, UserRepository userRepository, DatabaseHealthMonitor databaseHealthMonitor) {
        this.store = store;
        this.userRepository = userRepository;
        this.databaseHealthMonitor = databaseHealthMonitor;
    }

    @Scheduled(fixedDelay = 10000)
    public void flush() throws IOException {
        if (!databaseHealthMonitor.isHealthy()) {
            return;
        }

        List<Path> pending = store.listPending();
        for (Path path : pending) {
            UserDto payload = store.read(path);
            try {
                userRepository.save(payload.toEntity());
                store.markProcessed(path);
            } catch (DataIntegrityViolationException e) {
                store.markFailed(path);
            } catch (DataAccessException | TransactionException e) {
                return;
            }
        }
    }
}
