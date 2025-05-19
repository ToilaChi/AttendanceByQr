package com.example.userservice.repository;

import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {
  User findByCIC(String cic);

  User findByFullName(String fullName);

  List<User> findByClassCodeAndRole(String classCode, Role role);
}
