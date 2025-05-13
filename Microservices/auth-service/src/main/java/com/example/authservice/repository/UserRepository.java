package com.example.authservice.repository;

import com.example.authservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findUserByFirstName(String firstName);
  boolean existsByFirstName(String firstName);
  User findByCIC(String cic);
}
