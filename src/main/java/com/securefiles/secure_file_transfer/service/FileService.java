package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.FileStatus;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.FileRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileService {

  private static final Set<String> ALLOWED_EXTENSIONS =
      Set.of(".png", ".jpg", ".jpeg", ".pdf", ".doc", ".docx");

  private final CryptoService crypto;
  private final FileRecordRepository repo;

  @Value("${app.storage.base-dir:storage}")
  private String storageBaseDir;

  @Value("${app.storage.nodes:node1,node2,node3}")
  private String storageNodesRaw;

  @Value("${app.storage.replication-factor:2}")
  private int replicationFactor;

  @Value("${app.files.maxSizeBytes:52428800}")
  private long maxSizeBytes;

  private final ActivityService activityService;

  public FileService(FileRecordRepository repo, CryptoService crypto, ActivityService activityService) {
    this.repo = repo;
    this.crypto = crypto;
    this.activityService = activityService;
  }

  public FileRecord upload(User owner, MultipartFile file) throws Exception {
    validateUpload(file);

    long usedBytes = repo.findByOwner(owner).stream()
        .mapToLong(FileRecord::getSizeBytes)
        .sum();

    long maxBytes = 5L * 1024 * 1024 * 1024; // 5 GB

    if (usedBytes + file.getSize() > maxBytes) {
      throw new IllegalArgumentException("Storage quota exceeded. Max allowed is 60 GB.");
    }

    long totalStart = System.nanoTime();
    String originalFilename = safeOriginalFilename(file.getOriginalFilename());

    long hashStart = System.nanoTime();
    String sha256;
    try (InputStream in = file.getInputStream()) {
      sha256 = sha256(in);
    }
    long hashMs = nanosToMs(System.nanoTime() - hashStart);

    if (repo.existsByOwnerAndSha256(owner, sha256)) {
      throw new IllegalArgumentException("This file already exists in your storage");
    }

    String storedName = UUID.randomUUID() + ".bin";
    List<String> selectedNodes = chooseReplicaNodes();

    if (selectedNodes.isEmpty()) {
      throw new IllegalStateException("No storage nodes available");
    }

    List<Path> writtenPaths = new ArrayList<>();

    try {
      long encryptStart = System.nanoTime();

      byte[] plaintextBytes;
      try (InputStream in = file.getInputStream()) {
        plaintextBytes = in.readAllBytes();
      }

      String ivB64 = null;

      for (String node : selectedNodes) {
        Path nodeDir = Paths.get(storageBaseDir, node);
        Files.createDirectories(nodeDir);

        Path target = nodeDir.resolve(storedName);

        try (InputStream replicaIn = new java.io.ByteArrayInputStream(plaintextBytes)) {
          String currentIv = crypto.encryptToFile(replicaIn, target);
          if (ivB64 == null) {
            ivB64 = currentIv;
          }
        }

        writtenPaths.add(target);
      }

      long encryptMs = nanosToMs(System.nanoTime() - encryptStart);

      FileRecord rec = new FileRecord();
      rec.setOwner(owner);
      rec.setOriginalFilename(originalFilename);
      rec.setSha256(sha256);
      rec.setSizeBytes(file.getSize());
      rec.setStoredFilename(storedName);
      rec.setIvBase64(ivB64);
      rec.setEncrypted(true);
      rec.setContentType(file.getContentType());
      rec.setEncryptionAlgorithm("AES/GCM/NoPadding");
      rec.setStatus(FileStatus.STORED);
      rec.setUploadedAt(LocalDateTime.now());
      rec.setReplicaNodes(String.join(",", selectedNodes));

      FileRecord saved = repo.save(rec);
      activityService.log(
          owner,
          "UPLOAD",
          saved.getId(),
          saved.getOriginalFilename(),
          "Uploaded file: " + saved.getOriginalFilename()
      );
      long totalMs = nanosToMs(System.nanoTime() - totalStart);
      System.out.println("[UPLOAD] file=" + originalFilename +
          " size=" + file.getSize() +
          "B nodes=" + selectedNodes +
          " hashMs=" + hashMs +
          " encryptMs=" + encryptMs +
          " totalMs=" + totalMs);

      return saved;

    } catch (Exception e) {
      for (Path p : writtenPaths) {
        try {
          Files.deleteIfExists(p);
        } catch (Exception ignored) {
        }
      }
      throw e;
    }
  }

  public void streamDecryptedFile(FileRecord rec, OutputStream out) throws Exception {
    Path cipherPath = findFirstAvailableReplica(rec);

    if (cipherPath == null) {
      throw new IllegalArgumentException("Encrypted file missing on all storage nodes");
    }

    long start = System.nanoTime();
    crypto.decryptToStream(cipherPath, rec.getIvBase64(), out);
    long totalMs = nanosToMs(System.nanoTime() - start);

    rec.setDownloadCount(rec.getDownloadCount() + 1);
    rec.setLastAccessedAt(LocalDateTime.now());
    repo.save(rec);

    System.out.println("[DOWNLOAD] fileId=" + rec.getId() +
        " name=" + rec.getOriginalFilename() +
        " source=" + cipherPath +
        " size=" + rec.getSizeBytes() +
        "B decryptMs=" + totalMs);
  }

  @Transactional
  public void deleteFile(Long fileId, String username) {
    FileRecord rec = repo.findById(fileId)
        .orElseThrow(() -> new IllegalArgumentException("File not found"));

    if (rec.getOwner() == null || rec.getOwner().getUsername() == null) {
      throw new IllegalArgumentException("File owner is missing");
    }

    if (!rec.getOwner().getUsername().equals(username)) {
      throw new IllegalArgumentException("You can only delete your own files");
    }

    List<String> nodes = parseReplicaNodes(rec.getReplicaNodes());

    for (String node : nodes) {
      Path p = Paths.get(storageBaseDir, node, rec.getStoredFilename());
      try {
        Files.deleteIfExists(p);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to delete replica from node: " + node, e);
      }
    }

    rec.setStatus(FileStatus.DELETED);
    repo.delete(rec);

    activityService.log(
        rec.getOwner(),
        "DELETE",
        rec.getId(),
        rec.getOriginalFilename(),
        "Deleted file: " + rec.getOriginalFilename()
    );
  }

  public boolean hasReplicaAvailable(FileRecord rec) {
    return findFirstAvailableReplica(rec) != null;
  }

  private Path findFirstAvailableReplica(FileRecord rec) {
    List<String> nodes = parseReplicaNodes(rec.getReplicaNodes());
    for (String node : nodes) {
      Path p = Paths.get(storageBaseDir, node, rec.getStoredFilename());
      if (Files.exists(p)) {
        return p;
      }
    }
    return null;
  }

  private List<String> chooseReplicaNodes() {
    List<String> allNodes = Arrays.stream(storageNodesRaw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toList());

    if (allNodes.isEmpty()) {
      return List.of();
    }

    int count = Math.min(replicationFactor, allNodes.size());
    return new ArrayList<>(allNodes.subList(0, count));
  }

  private List<String> parseReplicaNodes(String replicaNodes) {
    if (replicaNodes == null || replicaNodes.isBlank()) {
      return List.of("node1");
    }
    return Arrays.stream(replicaNodes.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }

  private void validateUpload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    String original = file.getOriginalFilename();
    if (original == null || original.isBlank()) {
      throw new IllegalArgumentException("Filename is missing");
    }

    if (file.getSize() > maxSizeBytes) {
      throw new IllegalArgumentException("File exceeds maximum allowed size");
    }

    String lower = original.toLowerCase();
    boolean allowed = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    if (!allowed) {
      throw new IllegalArgumentException("Unsupported file type");
    }
  }

  private String safeOriginalFilename(String name) {
    if (name == null) {
      return "unknown";
    }
    return Path.of(name).getFileName().toString().replaceAll("[\\r\\n\"]", "_");
  }

  private String sha256(InputStream in) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] buf = new byte[8192];
    int r;
    while ((r = in.read(buf)) != -1) {
      md.update(buf, 0, r);
    }
    return HexFormat.of().formatHex(md.digest());
  }

  private long nanosToMs(long nanos) {
    return nanos / 1_000_000;
  }
}