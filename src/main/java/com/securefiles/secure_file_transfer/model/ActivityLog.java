package com.securefiles.secure_file_transfer.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, length = 50)
  private String actionType;

  @Column(length = 255)
  private String filename;

  @Column
  private Long fileId;

  @Column(nullable = false, length = 255)
  private String message;

  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public Long getId() { return id; }

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }

  public String getActionType() { return actionType; }
  public void setActionType(String actionType) { this.actionType = actionType; }

  public String getFilename() { return filename; }
  public void setFilename(String filename) { this.filename = filename; }

  public Long getFileId() { return fileId; }
  public void setFileId(Long fileId) { this.fileId = fileId; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}