package com.securefiles.secure_file_transfer.repository;

import com.securefiles.secure_file_transfer.model.ShareRequest;
import com.securefiles.secure_file_transfer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareRequestRepository extends JpaRepository<ShareRequest, Long> {

  List<ShareRequest> findByToUserAndStatus(User toUser, ShareRequest.Status status);

  List<ShareRequest> findByFromUserAndStatus(User fromUser, ShareRequest.Status status);

  boolean existsByFile_IdAndToUserAndStatus(Long fileId, User toUser, ShareRequest.Status status);

  // for download access check:
  boolean existsByFile_IdAndToUserAndStatusAndFromUser(Long fileId, User toUser, ShareRequest.Status status, User fromUser);

  //Delete all share requests where this user is involved (incoming/outgoing)
  void deleteByFromUserOrToUser(User fromUser, User toUser);

  //Delete all share requests for files owned by this user
  void deleteByFile_Owner(User owner);
}
