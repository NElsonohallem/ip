package com.securefiles.secure_file_transfer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_share_links")
public class FileShareLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "file_id")
  private FileRecord file;

  @Column(nullable = false, unique = true, length = 120)
  private String token;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private int maxDownloads = 10;

  @Column(nullable = false)
  private int currentDownloads = 0;

  private LocalDateTime expiresAt;

  @Column(length = 120)
  private String passwordHash;

  public Long getId() {
    return id;
  }

  public FileRecord getFile() {
    return file;
  }

  public void setFile(FileRecord file) {
    this.file = file;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public int getMaxDownloads() {
    return maxDownloads;
  }

  public void setMaxDownloads(int maxDownloads) {
    this.maxDownloads = maxDownloads;
  }

  public int getCurrentDownloads() {
    return currentDownloads;
  }

  public void setCurrentDownloads(int currentDownloads) {
    this.currentDownloads = currentDownloads;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }
}