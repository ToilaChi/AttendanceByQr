package com.example.authservice.models;

import com.nimbusds.openid.connect.sdk.claims.Gender;
import jakarta.persistence.*;
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
  @Enumerated(EnumType.STRING)
  private Role role;

  @Column
  private LocalDate createdAt;
}
