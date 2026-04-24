package com.securefiles.secure_file_transfer.controller;

import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import com.securefiles.secure_file_transfer.service.AccountService;
import com.securefiles.secure_file_transfer.service.JwtService;
import com.securefiles.secure_file_transfer.service.PasswordResetService;
import com.securefiles.secure_file_transfer.service.UserService;
import com.securefiles.secure_file_transfer.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService svc;
  private final JwtService jwt;
  private final VerificationService verificationService;
  private final UserRepository userRepo;
  private final PasswordResetService passwordResetService;
  private final AccountService accountService;


  public AuthController(
      UserService svc,
      JwtService jwt,
      VerificationService verificationService,
      UserRepository userRepo, PasswordResetService passwordResetService,
      AccountService accountService
  ) {
    this.svc = svc;
    this.jwt = jwt;
    this.verificationService = verificationService;
    this.userRepo = userRepo;
    this.passwordResetService = passwordResetService;
    this.accountService = accountService;
  }

  public record RegisterReq(String username, String email, String password) {}
  public record LoginReq(String username, String password) {}
  public record LoginRes(Long userId, String username, String token, String message) {}
  public record ResendReq(String email) {}
  public record VerifyCodeReq(String email, String code) {}
  public record DeleteAccountReq(String passwordConfirm) {}

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterReq req) {
    try {
      User u = svc.register(req.username(), req.email(), req.password());
      return ResponseEntity.ok(
          "Registered user id=" + u.getId() + ". Verification code sent. Please check your inbox."
      );
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginReq req) {
    try {
      User u = svc.login(req.username(), req.password());
      String token = jwt.issueToken(u.getUsername());
      return ResponseEntity.ok(new LoginRes(u.getId(), u.getUsername(), token, "Login successful"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(401).body(e.getMessage());
    }
  }

  @PostMapping("/verify-code")
  public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeReq req) {
    try {
      verificationService.verifyCode(req.email(), req.code());
      return ResponseEntity.ok("Email verified. You can now login.");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PostMapping("/resend-verification")
  public ResponseEntity<?> resend(@RequestBody ResendReq req) {
    try {
      User u = userRepo.findByEmail(req.email())
          .orElseThrow(() -> new IllegalArgumentException("No user with that email"));

      if (u.isEnabled()) return ResponseEntity.ok("Account already verified.");

      verificationService.sendVerification(u);
      return ResponseEntity.ok("Verification code sent.");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
  public record ForgotReq(String email) {}
  public record ResetConfirmReq(String email, String code, String newPassword) {}

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgot(@RequestBody ForgotReq req) {
    // always OK (avoid account enumeration)
    passwordResetService.requestReset(req.email());
    return ResponseEntity.ok("If that email exists, a reset code has been sent.");
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> reset(@RequestBody ResetConfirmReq req) {
    try {
      passwordResetService.confirmReset(req.email(), req.code(), req.newPassword());
      return ResponseEntity.ok("Password reset successful. You can now login.");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }


  @DeleteMapping("/delete-account")
  public ResponseEntity<?> deleteAccount(@RequestBody DeleteAccountReq req) {
    try {
      String username = SecurityContextHolder.getContext().getAuthentication().getName();
      accountService.deleteMyAccount(username, req.passwordConfirm());
      return ResponseEntity.ok("Account deleted.");
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Delete failed.");
    }
  }
}