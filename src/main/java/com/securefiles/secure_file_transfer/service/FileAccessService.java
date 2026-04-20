package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.ShareRequest;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.ShareRequestRepository;
import org.springframework.stereotype.Service;

@Service
public class FileAccessService {

  private final ShareRequestRepository shareRepo;

  public FileAccessService(ShareRequestRepository shareRepo) {
    this.shareRepo = shareRepo;
  }

  public boolean canDownload(User user, FileRecord rec) {
    boolean isOwner = rec.getOwner() != null && rec.getOwner().getId().equals(user.getId());

    boolean isSharedWithUser = shareRepo.existsByFile_IdAndToUserAndStatus(
        rec.getId(),
        user,
        ShareRequest.Status.APPROVED
    );

    return isOwner || isSharedWithUser;
  }
}