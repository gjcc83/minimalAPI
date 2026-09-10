package com.example.javaminimalapi.repository;

import com.example.javaminimalapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
