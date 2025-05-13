package com.example.authservice.models;

import com.nimbusds.openid.connect.sdk.claims.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
  @Id
  private UUID id;

  @Column
  private String username;

  @Column
  private String password;

  @Column
  private String CIC;

  @Column
  private Role role;

  @Column
  private LocalDate createdAt;
}
