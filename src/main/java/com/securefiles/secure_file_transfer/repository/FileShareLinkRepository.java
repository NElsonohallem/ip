package com.securefiles.secure_file_transfer.repository;

import com.securefiles.secure_file_transfer.model.FileShareLink;
import com.securefiles.secure_file_transfer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileShareLinkRepository extends JpaRepository<FileShareLink, Long> {

  Optional<FileShareLink> findByToken(String token);

  List<FileShareLink> findByFile_OwnerOrderByIdDesc(User owner);
}