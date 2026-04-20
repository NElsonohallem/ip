package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.FileRecordRepository;
import com.securefiles.secure_file_transfer.repository.ShareRequestRepository;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class AccountService {

  private final UserRepository userRepo;
  private final FileRecordRepository fileRepo;
  private final ShareRequestRepository shareRepo;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.storage.dir:storage}")
  private String storageDir;

  public AccountService(
      UserRepository userRepo,
      FileRecordRepository fileRepo,
      ShareRequestRepository shareRepo,
      PasswordEncoder passwordEncoder
  ) {
    this.userRepo = userRepo;
    this.fileRepo = fileRepo;
    this.shareRepo = shareRepo;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public void deleteMyAccount(String username, String passwordConfirm) {
    User me = userRepo.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

    // ✅ confirm password (IMPORTANT)
    // Change getPassword() if your field is named differently
    if (!passwordEncoder.matches(passwordConfirm, me.getPasswordHash())) {
      throw new IllegalArgumentException("Wrong password.");
    }

    // 1) delete share requests involving me (incoming/outgoing)
    shareRepo.deleteByFromUserOrToUser(me, me);

    // 2) delete share requests tied to my files (safety)
    shareRepo.deleteByFile_Owner(me);

    // 3) delete encrypted files from disk + db rows
    List<FileRecord> myFiles = fileRepo.findByOwner(me);
    for (FileRecord f : myFiles) {
      try {
        Path p = Path.of(storageDir).resolve(f.getStoredFilename());
        Files.deleteIfExists(p);
      } catch (Exception ignored) {}
    }
    fileRepo.deleteByOwner(me);

    // 4) finally delete the user
    userRepo.delete(me);
  }
}