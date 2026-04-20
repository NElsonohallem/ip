package com.securefiles.secure_file_transfer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    }
)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String username;

  @Column(nullable = false, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(nullable = false)
  private boolean enabled = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  // ✅ 6-digit verification code fields
  @Column(name = "verification_code", length = 6)
  private String verificationCode;

  @Column(name = "verification_code_expires_at")
  private LocalDateTime verificationCodeExpiresAt;

  // ---------------- getters/setters ----------------

  public Long getId() { return id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public String getVerificationCode() { return verificationCode; }
  public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

  public LocalDateTime getVerificationCodeExpiresAt() { return verificationCodeExpiresAt; }
  public void setVerificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) {
    this.verificationCodeExpiresAt = verificationCodeExpiresAt;
  }
}