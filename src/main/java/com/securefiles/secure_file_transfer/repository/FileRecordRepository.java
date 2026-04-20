package com.securefiles.secure_file_transfer.repository;

import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
  List<FileRecord> findByOwner(User owner);
  Optional<FileRecord> findByIdAndOwner(Long id, User owner);
  boolean existsByOwnerAndSha256(User owner, String sha256);
  void deleteByOwner(User owner);

  long countByOwner(User owner);

  @Query("select coalesce(sum(f.sizeBytes), 0) from FileRecord f where f.owner = :owner")
  long sumSizeBytesByOwner(User owner);
}