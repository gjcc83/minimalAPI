package com.example.javaminimalapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JavaMinimalApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaMinimalApiApplication.class, args);
    }
}
