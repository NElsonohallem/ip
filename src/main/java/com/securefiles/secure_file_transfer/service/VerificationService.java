package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class VerificationService {

  private final UserRepository userRepo;
  private final JavaMailSender mailSender;

  private final SecureRandom rng = new SecureRandom();

  public VerificationService(UserRepository userRepo, JavaMailSender mailSender) {
    this.userRepo = userRepo;
    this.mailSender = mailSender;
  }

  // ✅ Send 6-digit code (and store it on the user)
  public void sendVerification(User user) {
    // If already enabled, don’t spam.
    if (user.isEnabled()) return;

    String code = String.format("%06d", rng.nextInt(1_000_000));
    user.setVerificationCode(code);
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
    userRepo.save(user);

    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(user.getEmail());
    msg.setSubject("Verify your Secure File Transfer account");
    msg.setText(
        "Hi " + user.getUsername() + ",\n\n" +
            "Your 6-digit verification code is:\n\n" +
            code + "\n\n" +
            "This code expires in 10 minutes.\n\n" +
            "If you did not create this account, you can ignore this email."
    );

    mailSender.send(msg);
  }

  // ✅ Verify 6-digit code (email + code)
  public void verifyCode(String email, String code) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    if (code == null || !code.matches("\\d{6}")) {
      throw new IllegalArgumentException("Code must be exactly 6 digits");
    }

    User user = userRepo.findByEmail(email.trim())
        .orElseThrow(() -> new IllegalArgumentException("No user with that email"));

    if (user.isEnabled()) {
      return; // already verified
    }

    String expected = user.getVerificationCode();
    LocalDateTime exp = user.getVerificationCodeExpiresAt();

    if (expected == null || exp == null) {
      throw new IllegalArgumentException("No verification code found. Please resend a code.");
    }

    if (exp.isBefore(LocalDateTime.now())) {
      // expire the code so they must resend
      user.setVerificationCode(null);
      user.setVerificationCodeExpiresAt(null);
      userRepo.save(user);
      throw new IllegalArgumentException("Verification code expired. Please resend a new code.");
    }

    if (!expected.equals(code)) {
      throw new IllegalArgumentException("Invalid verification code");
    }

    // ✅ success
    user.setEnabled(true);
    user.setVerificationCode(null);
    user.setVerificationCodeExpiresAt(null);
    userRepo.save(user);
  }
}