package com.securefiles.secure_file_transfer.controller;

import com.securefiles.secure_file_transfer.dto.FileResponseDtos.FileListItemResponse;
import com.securefiles.secure_file_transfer.dto.FileResponseDtos.MessageResponse;
import com.securefiles.secure_file_transfer.dto.FileResponseDtos.UploadResponse;
import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.FileShareLink;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.FileRecordRepository;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import com.securefiles.secure_file_transfer.service.FileAccessService;
import com.securefiles.secure_file_transfer.service.FileService;
import com.securefiles.secure_file_transfer.service.FileShareLinkService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.securefiles.secure_file_transfer.dto.StorageStatsResponse;
import com.securefiles.secure_file_transfer.service.ActivityService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

  private final FileService fileService;
  private final FileAccessService fileAccessService;
  private final UserRepository userRepo;
  private final FileRecordRepository fileRepo;
  private final ActivityService activityService;
  private FileShareLinkService linkService;

  public FileController(
      FileService fileService,
      FileAccessService fileAccessService,
      UserRepository userRepo,
      FileRecordRepository fileRepo,
      ActivityService activityService
  ) {
    this.fileService = fileService;
    this.fileAccessService = fileAccessService;
    this.userRepo = userRepo;
    this.fileRepo = fileRepo;
    this.activityService = activityService;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalArgumentException("Not authenticated");
    }
    return auth.getName();
  }

  @PostMapping("/upload")
  public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws Exception {
    String username = currentUsername();

    User owner = userRepo.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

    FileRecord saved = fileService.upload(owner, file);

    return ResponseEntity.ok(
        new UploadResponse(
            saved.getId(),
            saved.getOriginalFilename(),
            saved.getSizeBytes(),
            "Upload successful"
        )
    );
  }

  @GetMapping("/list")
  public ResponseEntity<List<FileListItemResponse>> list() {
    String username = currentUsername();

    User owner = userRepo.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

    List<FileListItemResponse> result = fileRepo.findByOwner(owner)
        .stream()
        .map(f -> new FileListItemResponse(
            f.getId(),
            f.getOriginalFilename() != null ? f.getOriginalFilename() : "unknown",
            f.getSizeBytes(),
            f.getContentType() != null ? f.getContentType() : "application/octet-stream",
            f.getDownloadCount(),
            f.getUploadedAt(),
            f.getStatus() != null ? f.getStatus().name() : "STORED"
        ))
        .toList();

    return ResponseEntity.ok(result);
  }

  @GetMapping("/{token}/download")
  public void download(@PathVariable String token, HttpServletResponse response) throws Exception {
    FileShareLink link = linkService.requireValidLink(token);
    FileRecord rec = link.getFile();

    if (!fileService.hasReplicaAvailable(rec)) {
      response.reset();
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("""
            {"error":"File not found","message":"Encrypted file missing on all storage nodes"}
        """);
      return;
    }

    String filename = rec.getOriginalFilename();
    String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

    response.setContentType(
        rec.getContentType() != null ? rec.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE
    );
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename*=UTF-8''" + encodedFilename
    );

    if (rec.getSizeBytes() > 0) {
      response.setContentLengthLong(rec.getSizeBytes());
    }

    fileService.streamDecryptedFile(rec, response.getOutputStream());
    linkService.markDownloaded(link);
  }
   

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<MessageResponse> deleteFile(@PathVariable Long id, Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new IllegalArgumentException("Not authenticated");
    }
    String username = auth.getName();
    fileService.deleteFile(id, username);
    return ResponseEntity.ok(new MessageResponse("File deleted"));
  }

  @GetMapping("/stats")
  public ResponseEntity<StorageStatsResponse> stats() {
    String username = currentUsername();

    User owner = userRepo.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

    List<FileRecord> files = fileRepo.findByOwner(owner);

    long totalFiles = files.size();
    long totalBytes = files.stream().mapToLong(FileRecord::getSizeBytes).sum();
    long totalDownloads = files.stream().mapToLong(FileRecord::getDownloadCount).sum();

    long imageCount = files.stream()
        .filter(f -> {
          String ct = f.getContentType();
          String name = f.getOriginalFilename().toLowerCase();
          return (ct != null && ct.startsWith("image/"))
              || name.endsWith(".png")
              || name.endsWith(".jpg")
              || name.endsWith(".jpeg");
        })
        .count();

    long documentCount = files.stream()
        .filter(f -> {
          String name = f.getOriginalFilename().toLowerCase();
          return name.endsWith(".pdf")
              || name.endsWith(".doc")
              || name.endsWith(".docx");
        })
        .count();

    long otherCount = totalFiles - imageCount - documentCount;
    double usedMb = totalBytes / (1024.0 * 1024.0);

    return ResponseEntity.ok(new StorageStatsResponse(
        totalFiles,
        totalBytes,
        imageCount,
        documentCount,
        otherCount,
        totalDownloads,
        usedMb
    ));

  }
}