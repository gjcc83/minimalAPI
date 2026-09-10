package com.example.javaminimalapi.web;

import com.example.javaminimalapi.dto.ErrorResponse;
import com.example.javaminimalapi.dto.UserDto;
import com.example.javaminimalapi.entity.User;
import com.example.javaminimalapi.repository.UserRepository;
import com.example.javaminimalapi.resilience.PendingUserWriteStore;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserHandler {

    private final UserRepository userRepository;
    private final Validator validator;
    private final PendingUserWriteStore pendingUserWriteStore;
    private final CircuitBreaker circuitBreaker;

    public UserHandler(
            UserRepository userRepository,
            Validator validator,
            PendingUserWriteStore pendingUserWriteStore,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.userRepository = userRepository;
        this.validator = validator;
        this.pendingUserWriteStore = pendingUserWriteStore;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("database");
    }

    public ServerResponse createUser(ServerRequest request) throws Exception {
        UserDto payload = request.body(UserDto.class);
        Set<ConstraintViolation<UserDto>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(message));
        }

        User saved;
        try {
            saved = circuitBreaker.executeSupplier(() -> userRepository.save(payload.toEntity()));
        } catch (DataIntegrityViolationException e) {
            return ServerResponse.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse("email already in use: " + payload.email()));
        } catch (CallNotPermittedException | DataAccessException | TransactionException e) {
            pendingUserWriteStore.enqueue(payload);
            return queuedResponse();
        }
        return ServerResponse.created(URI.create("/api/users/" + saved.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(UserDto.from(saved));
    }

    private static ServerResponse queuedResponse() {
        return ServerResponse.status(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("database unavailable, request queued for processing once it recovers"));
    }

    public ServerResponse getUser(ServerRequest request) throws Exception {
        Long id = Long.valueOf(request.pathVariable("id"));
        try {
            return circuitBreaker.executeSupplier(() -> userRepository.findById(id))
                    .map(user -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(UserDto.from(user)))
                    .orElseGet(() -> ServerResponse.notFound().build());
        } catch (CallNotPermittedException | DataAccessException | TransactionException e) {
            return serviceUnavailable();
        }
    }

    public ServerResponse getAllUsers(ServerRequest request) throws Exception {
        try {
            List<UserDto> users = circuitBreaker.executeSupplier(() -> userRepository.findAll())
                    .stream().map(UserDto::from).toList();
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(users);
        } catch (CallNotPermittedException | DataAccessException | TransactionException e) {
            return serviceUnavailable();
        }
    }

    static ServerResponse serviceUnavailable() {
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("service temporarily unavailable, please try again later"));
    }
}
