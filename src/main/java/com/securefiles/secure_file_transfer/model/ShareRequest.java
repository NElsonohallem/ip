package com.securefiles.secure_file_transfer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "share_requests",
    uniqueConstraints = {
        // stops duplicate pending requests for same file to same user
        @UniqueConstraint(columnNames = {"file_id", "to_user_id", "status"})
    }
)
public class ShareRequest {

  public enum Status { PENDING, APPROVED, REJECTED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "file_id")
  private FileRecord file;

  @ManyToOne(optional = false)
  @JoinColumn(name = "from_user_id")
  private User fromUser;

  @ManyToOne(optional = false)
  @JoinColumn(name = "to_user_id")
  private User toUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status = Status.PENDING;

  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime respondedAt;

  public Long getId() { return id; }

  public FileRecord getFile() { return file; }
  public void setFile(FileRecord file) { this.file = file; }

  public User getFromUser() { return fromUser; }
  public void setFromUser(User fromUser) { this.fromUser = fromUser; }

  public User getToUser() { return toUser; }
  public void setToUser(User toUser) { this.toUser = toUser; }

  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }

  public LocalDateTime getCreatedAt() { return createdAt; }

  public LocalDateTime getRespondedAt() { return respondedAt; }
  public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
