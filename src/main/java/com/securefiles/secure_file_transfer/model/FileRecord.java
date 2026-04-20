package com.securefiles.secure_file_transfer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "files",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"owner_id", "sha256"})
    }
)
public class FileRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(nullable = false, length = 255)
  private String originalFilename;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 255)
  private String storedFilename;

  @Column(nullable = false, length = 24)
  private String ivBase64;

  @Column(nullable = false)
  private boolean encrypted = true;

  @Column(length = 100)
  private String contentType;

  @Column(nullable = false, length = 50)
  private String encryptionAlgorithm = "AES/GCM/NoPadding";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FileStatus status = FileStatus.STORED;

  @Column(nullable = false)
  private long downloadCount = 0;

  @Column(nullable = false, length = 255)
  private String replicaNodes;

  @Column(nullable = false)
  private LocalDateTime uploadedAt = LocalDateTime.now();

  private LocalDateTime lastAccessedAt;

  public Long getId() { return id; }

  public User getOwner() { return owner; }
  public void setOwner(User owner) { this.owner = owner; }

  public String getOriginalFilename() { return originalFilename; }
  public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

  public String getSha256() { return sha256; }
  public void setSha256(String sha256) { this.sha256 = sha256; }

  public long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

  public String getStoredFilename() { return storedFilename; }
  public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }

  public String getIvBase64() { return ivBase64; }
  public void setIvBase64(String ivBase64) { this.ivBase64 = ivBase64; }

  public boolean isEncrypted() { return encrypted; }
  public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }

  public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
  public void setEncryptionAlgorithm(String encryptionAlgorithm) { this.encryptionAlgorithm = encryptionAlgorithm; }

  public FileStatus getStatus() { return status; }
  public void setStatus(FileStatus status) { this.status = status; }

  public long getDownloadCount() { return downloadCount; }
  public void setDownloadCount(long downloadCount) { this.downloadCount = downloadCount; }

  public String getReplicaNodes() { return replicaNodes; }
  public void setReplicaNodes(String replicaNodes) { this.replicaNodes = replicaNodes; }

  public LocalDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

  public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
  public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
}