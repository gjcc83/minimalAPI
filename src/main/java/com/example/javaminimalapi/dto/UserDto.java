package com.example.javaminimalapi.dto;

import com.example.javaminimalapi.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDto(
        Long id,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "lastName is required") String lastName,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getName(), user.getLastName(), user.getEmail());
    }

    public User toEntity() {
        return new User(name, lastName, email);
    }
}
