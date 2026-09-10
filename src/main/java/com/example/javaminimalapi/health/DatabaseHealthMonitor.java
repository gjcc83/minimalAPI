package com.example.javaminimalapi.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

// Probes on its own raw connection (not the Hikari pool) so it stays fast-failing regardless of the pool's connection-timeout.
@Component
public class DatabaseHealthMonitor {

    private static final int PROBE_TIMEOUT_SECONDS = 2;

    private final String url;
    private final String username;
    private final String password;
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    public DatabaseHealthMonitor(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkHealth() {
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("connectTimeout", String.valueOf(PROBE_TIMEOUT_SECONDS));

        try (Connection connection = DriverManager.getConnection(url, props)) {
            healthy.set(connection.isValid(PROBE_TIMEOUT_SECONDS));
        } catch (SQLException e) {
            healthy.set(false);
        }
    }

    public boolean isHealthy() {
        return healthy.get();
    }
}
