package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.FileRecordRepository;
import com.securefiles.secure_file_transfer.repository.FileShareLinkRepository;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class AccountService {

  private final UserRepository userRepo;
  private final FileRecordRepository fileRepo;
  private final PasswordEncoder passwordEncoder;
  private final FileShareLinkRepository shareLinkRepo;
  private final String storageBaseDir = "storage";

  public AccountService(
      UserRepository userRepo,
      FileRecordRepository fileRepo,
      PasswordEncoder passwordEncoder, FileShareLinkRepository shareLinkRepo
  ) {
    this.userRepo = userRepo;
    this.fileRepo = fileRepo;
    this.passwordEncoder = passwordEncoder;
    this.shareLinkRepo = shareLinkRepo;
  }

  @Transactional
  public void deleteAccountAndWipeStorage(String username, String rawPassword) {
    User user = userRepo.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw new IllegalArgumentException("Incorrect password");
    }

    List<FileRecord> files = fileRepo.findByOwner(user);

    for (FileRecord file : files) {
      shareLinkRepo.deleteByFile(file);
      deletePhysicalReplicas(file);
    }

    fileRepo.deleteByOwner(user);
    userRepo.delete(user);
  }

  private void deletePhysicalReplicas(FileRecord file) {
    if (file.getStoredFilename() == null) return;

    String nodes = file.getReplicaNodes();

    if (nodes == null || nodes.isBlank()) {
      deleteOne(Paths.get(storageBaseDir, file.getStoredFilename()));
      return;
    }

    for (String node : nodes.split(",")) {
      deleteOne(Paths.get(storageBaseDir, node.trim(), file.getStoredFilename()));
    }
  }

  private void deleteOne(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (Exception ignored) {
    }
  }
}