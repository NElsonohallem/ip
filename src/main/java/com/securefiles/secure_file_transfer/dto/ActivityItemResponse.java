package com.securefiles.secure_file_transfer.dto;

import java.time.LocalDateTime;

public record ActivityItemResponse(
    Long id,
    String actionType,
    String filename,
    Long fileId,
    String message,
    LocalDateTime createdAt
) {}