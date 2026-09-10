package com.example.javaminimalapi.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.accept;

@Configuration
public class UserRouter {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler handler) {
        return RouterFunctions.route()
                .POST("/api/users", accept(MediaType.APPLICATION_JSON), handler::createUser)
                .GET("/api/users/{id}", handler::getUser)
                .GET("/api/users", handler::getAllUsers)
                .build();
    }
}
