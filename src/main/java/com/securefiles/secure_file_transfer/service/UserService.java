package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository repo;
  private final PasswordEncoder encoder;
  private final VerificationService verificationService;

  public UserService(UserRepository repo, PasswordEncoder encoder, VerificationService verificationService) {
    this.repo = repo;
    this.encoder = encoder;
    this.verificationService = verificationService;
  }

  public User register(String username, String email, String rawPassword) throws Exception {
    if (repo.existsByUsername(username)) throw new IllegalArgumentException("Username already taken");
    if (repo.existsByEmail(email)) throw new IllegalArgumentException("Email already registered");

    User u = new User();
    u.setUsername(username);
    u.setEmail(email);
    u.setPasswordHash(encoder.encode(rawPassword));
    u.setEnabled(false);

    User saved = repo.save(u);
    verificationService.sendVerification(saved); // ✅ send email
    return saved;
  }

  public User login(String username, String rawPassword) {
    User u = repo.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    if (!u.isEnabled()) {
      throw new IllegalArgumentException("Email not verified");
    }

    if (!encoder.matches(rawPassword, u.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    return u;
  }
}