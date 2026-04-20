package com.securefiles.secure_file_transfer.repository;

import com.securefiles.secure_file_transfer.model.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
  List<PasswordResetCode> findByUser_IdAndUsedFalse(Long userId);
  Optional<PasswordResetCode> findTopByUser_IdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}