package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.PasswordResetCode;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.PasswordResetCodeRepository;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {

  private final UserRepository userRepo;
  private final PasswordResetCodeRepository resetRepo;
  private final JavaMailSender mailSender;
  private final PasswordEncoder encoder;

  private final SecureRandom rng = new SecureRandom();

  public PasswordResetService(
      UserRepository userRepo,
      PasswordResetCodeRepository resetRepo,
      JavaMailSender mailSender,
      PasswordEncoder encoder
  ) {
    this.userRepo = userRepo;
    this.resetRepo = resetRepo;
    this.mailSender = mailSender;
    this.encoder = encoder;
  }

  // Always respond success (don’t leak whether email exists)
  public void requestReset(String email) {
    userRepo.findByEmail(email).ifPresent(user -> {
      // invalidate old codes
      for (PasswordResetCode c : resetRepo.findByUser_IdAndUsedFalse(user.getId())) {
        c.setUsed(true);
      }

      String code = generate6DigitCode();
      PasswordResetCode prc = new PasswordResetCode();
      prc.setUser(user);
      prc.setCodeHash(encoder.encode(code)); // store HASH, not raw code
      prc.setExpiresAt(LocalDateTime.now().plusMinutes(15));
      resetRepo.save(prc);

      sendResetEmail(user, code);
    });
  }

  public void confirmReset(String email, String code, String newPassword) {
    User user = userRepo.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Invalid reset code"));

    PasswordResetCode prc = resetRepo.findTopByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
        .orElseThrow(() -> new IllegalArgumentException("Invalid reset code"));

    if (prc.getExpiresAt().isBefore(LocalDateTime.now())) {
      prc.setUsed(true);
      resetRepo.save(prc);
      throw new IllegalArgumentException("Reset code expired. Request a new one.");
    }

    if (!encoder.matches(code, prc.getCodeHash())) {
      throw new IllegalArgumentException("Invalid reset code");
    }

    // update password
    user.setPasswordHash(encoder.encode(newPassword));
    userRepo.save(user);

    // mark code used
    prc.setUsed(true);
    resetRepo.save(prc);
  }

  private void sendResetEmail(User user, String code) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(user.getEmail());
    msg.setSubject("Reset your Secure File Transfer password");
    msg.setText(
        "Hi " + user.getUsername() + ",\n\n" +
            "Use this 6-digit code to reset your password:\n\n" +
            code + "\n\n" +
            "This code expires in 15 minutes.\n" +
            "If you didn't request this, you can ignore this email.\n"
    );
    mailSender.send(msg);
  }

  private String generate6DigitCode() {
    int n = rng.nextInt(1_000_000);
    return String.format("%06d", n);
  }
}