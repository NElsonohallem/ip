package com.securefiles.secure_file_transfer.dto;

import java.time.LocalDateTime;

public class FileResponseDtos {

  public record UploadResponse(
      Long id,
      String filename,
      long sizeBytes,
      String message
  ) {}

  public record FileListItemResponse(
      Long id,
      String filename,
      long sizeBytes,
      String contentType,
      long downloadCount,
      LocalDateTime uploadedAt,
      String status
  ) {}

  public record MessageResponse(
      String message
  ) {}

  public record ErrorResponse(
      String error,
      String message,
      String path,
      LocalDateTime timestamp
  ) {}
}