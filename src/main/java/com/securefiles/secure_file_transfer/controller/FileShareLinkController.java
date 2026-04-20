package com.securefiles.secure_file_transfer.controller;

import com.securefiles.secure_file_transfer.dto.ShareLinkDtos;
import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.FileShareLink;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import com.securefiles.secure_file_transfer.service.FileService;
import com.securefiles.secure_file_transfer.service.FileShareLinkService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/share-links")
public class FileShareLinkController {

  private final FileShareLinkService linkService;
  private final FileService fileService;
  private final UserRepository userRepo;

  public FileShareLinkController(
      FileShareLinkService linkService,
      FileService fileService,
      UserRepository userRepo
  ) {
    this.linkService = linkService;
    this.fileService = fileService;
    this.userRepo = userRepo;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalArgumentException("Not authenticated");
    }
    return auth.getName();
  }

  private User currentUser() {
    return userRepo.findByUsername(currentUsername())
        .orElseThrow(() -> new IllegalArgumentException("Unknown user"));
  }

  @PostMapping("/create/{fileId}")
  public ResponseEntity<ShareLinkDtos.ShareLinkResponse> create(
      @PathVariable Long fileId,
      @RequestBody(required = false) ShareLinkDtos.CreateShareLinkRequest req
  ) {
    User owner = currentUser();

    FileShareLink link = linkService.createLink(
        owner,
        fileId,
        req == null ? null : req.maxDownloads(),
        req == null ? null : req.expiresInDays()
    );

    return ResponseEntity.ok(linkService.toResponse(link));
  }

  @GetMapping("/mine")
  public ResponseEntity<?> mine() {
    User owner = currentUser();
    List<FileShareLink> links = linkService.findByOwner(owner);

    var result = links.stream().map(link -> {
      var m = new LinkedHashMap<String, Object>();
      m.put("id", link.getId());
      m.put("fileId", link.getFile().getId());
      m.put("filename", link.getFile().getOriginalFilename());
      m.put("token", link.getToken());
      m.put("url", "/api/share-links/" + link.getToken() + "/info");
      m.put("maxDownloads", link.getMaxDownloads());
      m.put("downloadCount", link.getCurrentDownloads());
      m.put("expiresAt", link.getExpiresAt());
      m.put("revoked", !link.isActive());
      return m;
    }).toList();

    return ResponseEntity.ok(result);
  }

  @GetMapping("/{token}/info")
  public ResponseEntity<ShareLinkDtos.PublicShareLinkInfoResponse> info(@PathVariable String token) {
    FileShareLink link = linkService.requireValidLink(token);
    FileRecord file = link.getFile();

    int remaining = Math.max(0, link.getMaxDownloads() - link.getCurrentDownloads());

    return ResponseEntity.ok(new ShareLinkDtos.PublicShareLinkInfoResponse(
        file.getOriginalFilename(),
        file.getSizeBytes(),
        file.getContentType(),
        link.isActive(),
        remaining,
        link.getExpiresAt()
    ));
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

  @DeleteMapping("/revoke/{linkId}")
  public ResponseEntity<?> revoke(@PathVariable Long linkId) {
    User owner = currentUser();
    linkService.revoke(owner, linkId);
    return ResponseEntity.ok("Share link revoked");
  }
}