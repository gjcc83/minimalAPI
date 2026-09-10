package com.example.javaminimalapi.resilience;

import com.example.javaminimalapi.dto.UserDto;
import com.example.javaminimalapi.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
    private final CircuitBreaker circuitBreaker;

    public PendingUserWriteFlusher(
            PendingUserWriteStore store, UserRepository userRepository, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.store = store;
        this.userRepository = userRepository;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("database");
    }

    @Scheduled(fixedDelay = 10000)
    public void flush() throws IOException {
        List<Path> pending = store.listPending();
        for (Path path : pending) {
            UserDto payload = store.read(path);
            try {
                circuitBreaker.executeSupplier(() -> userRepository.save(payload.toEntity()));
                store.markProcessed(path);
            } catch (DataIntegrityViolationException e) {
                store.markFailed(path);
            } catch (CallNotPermittedException | DataAccessException | TransactionException e) {
                return;
            }
        }
    }
}
