package com.securefiles.secure_file_transfer.dto;

public record StorageStatsResponse(
    long totalFiles,
    long totalBytes,
    long imageCount,
    long documentCount,
    long otherCount,
    long totalDownloads,
    double usedMb
) {}