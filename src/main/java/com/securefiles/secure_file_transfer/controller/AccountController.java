package com.securefiles.secure_file_transfer.controller;

import com.securefiles.secure_file_transfer.dto.DeleteAccountRequest;
import com.securefiles.secure_file_transfer.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @DeleteMapping("/me")
  public ResponseEntity<?> deleteMe(
      Authentication auth,
      @RequestBody DeleteAccountRequest request
  ) {
    if (auth == null || auth.getName() == null) {
      return ResponseEntity.status(401).body("Not authenticated");
    }

    accountService.deleteAccountAndWipeStorage(auth.getName(), request.password());

    return ResponseEntity.ok("Account deleted successfully");
  }
}