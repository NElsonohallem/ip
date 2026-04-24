package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
  public void sendVerification(User user) throws Exception {
    if (user.isEnabled()) return;

    String code = String.format("%06d", rng.nextInt(1_000_000));
    user.setVerificationCode(code);
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
    userRepo.save(user);

    String html = """
      <div style="font-family:Arial,sans-serif;background:#f6f8fb;padding:30px;">
        <div style="max-width:520px;margin:auto;background:white;border-radius:18px;padding:28px;border:1px solid #e5e7eb;">
          <h2 style="margin:0;color:#111827;">SecureSendr</h2>
          <p style="color:#6b7280;margin-top:6px;">Verify your account</p>

          <p style="font-size:16px;color:#111827;">Hi %s,</p>

          <p style="font-size:15px;color:#374151;">
            Use the verification code below to activate your account.
          </p>

          <div style="text-align:center;margin:28px 0;">
            <div style="display:inline-block;background:#84d53a;color:white;
                        font-size:34px;font-weight:bold;letter-spacing:8px;
                        padding:18px 28px;border-radius:14px;">
              %s
            </div>
          </div>

          <p style="font-size:14px;color:#6b7280;">
            This code expires in <b>10 minutes</b>.
          </p>

          <p style="font-size:13px;color:#9ca3af;margin-top:24px;">
            If you did not create this account, you can ignore this email.
          </p>
        </div>
      </div>
      """.formatted(user.getUsername(), code);

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    helper.setTo(user.getEmail());
    helper.setSubject("Verify your SecureSendr account");
    helper.setText(html, true);

    mailSender.send(message);
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