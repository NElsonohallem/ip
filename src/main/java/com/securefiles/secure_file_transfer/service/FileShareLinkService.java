package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.dto.ShareLinkDtos;
import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.FileShareLink;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.FileRecordRepository;
import com.securefiles.secure_file_transfer.repository.FileShareLinkRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileShareLinkService {

  private final FileShareLinkRepository linkRepo;
  private final FileRecordRepository fileRepo;
  private final ActivityService activityService;

  @Value("${app.frontend.baseUrl:http://localhost:8080}")
  private String frontendBaseUrl;

  public FileShareLinkService(
      FileShareLinkRepository linkRepo,
      FileRecordRepository fileRepo,
      ActivityService activityService
  ) {
    this.linkRepo = linkRepo;
    this.fileRepo = fileRepo;
    this.activityService = activityService;
  }

  @Transactional
  public FileShareLink createLink(User owner, Long fileId, Integer maxDownloads, Integer expiresInDays) {
    FileRecord file = fileRepo.findByIdAndOwner(fileId, owner)
        .orElseThrow(() -> new IllegalArgumentException("File not found or not owned by you"));

    FileShareLink link = new FileShareLink();
    link.setFile(file);
    link.setToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
    link.setActive(true);
    link.setCurrentDownloads(0);
    link.setMaxDownloads(maxDownloads == null || maxDownloads < 1 ? 10 : maxDownloads);
    link.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays == null || expiresInDays < 1 ? 7 : expiresInDays));

    FileShareLink saved = linkRepo.save(link);

    activityService.log(
        owner,
        "CREATE_LINK",
        file.getId(),
        file.getOriginalFilename(),
        "Created public share link for " + file.getOriginalFilename()
    );

    return saved;
  }

  public ShareLinkDtos.ShareLinkResponse toResponse(FileShareLink link) {
    return new ShareLinkDtos.ShareLinkResponse(
        link.getId(),
        link.getToken(),
        frontendBaseUrl + "/shared/" + link.getToken(),
        link.isActive(),
        link.getMaxDownloads(),
        link.getCurrentDownloads(),
        link.getExpiresAt(),
        link.getFile().getId(),
        link.getFile().getOriginalFilename()
    );
  }

  public FileShareLink requireValidLink(String token) {
    FileShareLink link = linkRepo.findByToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

    if (!link.isActive()) {
      throw new IllegalArgumentException("This link has been revoked");
    }

    if (link.getExpiresAt() != null && LocalDateTime.now().isAfter(link.getExpiresAt())) {
      throw new IllegalArgumentException("This link has expired");
    }

    if (link.getCurrentDownloads() >= link.getMaxDownloads()) {
      throw new IllegalArgumentException("Download limit reached");
    }

    return link;
  }

  @Transactional
  public void markDownloaded(FileShareLink link) {
    link.setCurrentDownloads(link.getCurrentDownloads() + 1);
    linkRepo.save(link);
  }

  @Transactional
  public void revoke(User owner, Long linkId) {
    FileShareLink link = linkRepo.findById(linkId)
        .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

    if (!link.getFile().getOwner().getId().equals(owner.getId())) {
      throw new IllegalArgumentException("Not allowed");
    }

    link.setActive(false);
    linkRepo.save(link);

    activityService.log(
        owner,
        "REVOKE_LINK",
        link.getFile().getId(),
        link.getFile().getOriginalFilename(),
        "Revoked public share link for " + link.getFile().getOriginalFilename()
    );
  }

  public List<FileShareLink> findByOwner(User owner) {
    return linkRepo.findByFile_OwnerOrderByIdDesc(owner);
  }
}