package com.securefiles.secure_file_transfer.dto;

import java.time.LocalDateTime;

public class ShareLinkDtos {

  public record CreateShareLinkRequest(
      Integer maxDownloads,
      Integer expiresInDays
  ) {}

  public record ShareLinkResponse(
      Long id,
      String token,
      String url,
      boolean active,
      int maxDownloads,
      int currentDownloads,
      LocalDateTime expiresAt,
      Long fileId,
      String filename
  ) {}

  public record PublicShareLinkInfoResponse(
      String filename,
      long sizeBytes,
      String contentType,
      boolean active,
      int remainingDownloads,
      LocalDateTime expiresAt
  ) {}
}