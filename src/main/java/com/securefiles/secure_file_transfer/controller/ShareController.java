package com.securefiles.secure_file_transfer.controller;

import com.securefiles.secure_file_transfer.model.FileRecord;
import com.securefiles.secure_file_transfer.model.ShareRequest;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.FileRecordRepository;
import com.securefiles.secure_file_transfer.repository.ShareRequestRepository;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import com.securefiles.secure_file_transfer.service.ActivityService;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/shares")
public class ShareController {

  private final ShareRequestRepository shareRepo;
  private final UserRepository userRepo;
  private final FileRecordRepository fileRepo;
  private final ActivityService activityService;

  public ShareController(ShareRequestRepository shareRepo, UserRepository userRepo, FileRecordRepository fileRepo,
      ActivityService activityService) {
    this.shareRepo = shareRepo;
    this.userRepo = userRepo;
    this.fileRepo = fileRepo;
    this.activityService = activityService;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) throw new IllegalArgumentException("Not authenticated");
    return auth.getName();
  }

  public record ShareRequestReq(Long fileId, String toUsername) {}

  // A -> request share to B
  @PostMapping("/request")
  public ResponseEntity<?> requestShare(@RequestBody ShareRequestReq req) {
    ShareRequest saved;
    try {
      String me = currentUsername();
      User from = userRepo.findByUsername(me)
          .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

      User to = userRepo.findByUsername(req.toUsername())
          .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

      if (from.getId().equals(to.getId())) {
        return ResponseEntity.badRequest().body("You cannot share to yourself");
      }

      FileRecord file = fileRepo.findByIdAndOwner(req.fileId(), from)
          .orElseThrow(() -> new IllegalArgumentException("File not found or not owned by you"));

      if (shareRepo.existsByFile_IdAndToUserAndStatus(file.getId(), to,
          ShareRequest.Status.PENDING)) {
        return ResponseEntity.badRequest().body("Share request already pending");
      }
      if (shareRepo.existsByFile_IdAndToUserAndStatus(file.getId(), to,
          ShareRequest.Status.APPROVED)) {
        return ResponseEntity.badRequest().body("Already shared and approved");
      }

      ShareRequest sr = new ShareRequest();
      sr.setFile(file);
      sr.setFromUser(from);
      sr.setToUser(to);
      sr.setStatus(ShareRequest.Status.PENDING);

      saved = shareRepo.save(sr);

      activityService.log(
          from,
          "SHARE_REQUEST",
          file.getId(),
          file.getOriginalFilename(),
          "Requested share for " + file.getOriginalFilename() + " to " + to.getUsername()
      );
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok("Share request created id=" + saved.getId());
  }

  // B -> view incoming pending requests
  @GetMapping("/incoming")
  public ResponseEntity<?> incoming() {
    try {
      String me = currentUsername();
      User to = userRepo.findByUsername(me).orElseThrow(() -> new IllegalArgumentException("Unknown user"));

      List<ShareRequest> reqs = shareRepo.findByToUserAndStatus(to, ShareRequest.Status.PENDING);

      var result = reqs.stream().map(r -> {
        var m = new LinkedHashMap<String, Object>();
        m.put("shareId", r.getId());
        m.put("fileId", r.getFile().getId());
        m.put("filename", r.getFile().getOriginalFilename());
        m.put("fromUser", r.getFromUser().getUsername());
        m.put("status", r.getStatus().name());
        m.put("createdAt", r.getCreatedAt().toString());
        return m;
      }).toList();

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // B -> approve
  @PostMapping("/approve/{shareId}")
  public ResponseEntity<?> approve(@PathVariable Long shareId) {
    return respond(shareId, ShareRequest.Status.APPROVED);
  }

  // B -> reject
  @PostMapping("/reject/{shareId}")
  public ResponseEntity<?> reject(@PathVariable Long shareId) {
    return respond(shareId, ShareRequest.Status.REJECTED);
  }

  private ResponseEntity<?> respond(Long shareId, ShareRequest.Status newStatus) {
    try {
      String me = currentUsername();
      User to = userRepo.findByUsername(me).orElseThrow(() -> new IllegalArgumentException("Unknown user"));

      ShareRequest sr = shareRepo.findById(shareId)
          .orElseThrow(() -> new IllegalArgumentException("Share request not found"));

      if (!sr.getToUser().getId().equals(to.getId())) {
        return ResponseEntity.status(403).body("Not allowed");
      }

      if (sr.getStatus() != ShareRequest.Status.PENDING) {
        return ResponseEntity.badRequest().body("Request already responded");
      }

      sr.setStatus(newStatus);
      sr.setRespondedAt(LocalDateTime.now());
      shareRepo.save(sr);

      String actionType = (newStatus == ShareRequest.Status.APPROVED)
          ? "SHARE_APPROVE"
          : "SHARE_REJECT";

      String message = (newStatus == ShareRequest.Status.APPROVED)
          ? "Approved share for " + sr.getFile().getOriginalFilename()
          : "Rejected share for " + sr.getFile().getOriginalFilename();

      activityService.log(
          to,
          actionType,
          sr.getFile().getId(),
          sr.getFile().getOriginalFilename(),
          message
      );
      return ResponseEntity.ok("Share request " + newStatus.name().toLowerCase());
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // B -> list files shared with me (approved)
  @GetMapping("/with-me")
  public ResponseEntity<?> sharedWithMe() {
    try {
      String me = currentUsername();
      User to = userRepo.findByUsername(me).orElseThrow(() -> new IllegalArgumentException("Unknown user"));

      List<ShareRequest> reqs = shareRepo.findByToUserAndStatus(to, ShareRequest.Status.APPROVED);

      var result = reqs.stream().map(r -> {
        var m = new LinkedHashMap<String, Object>();
        m.put("shareId", r.getId());
        m.put("fileId", r.getFile().getId());
        m.put("filename", r.getFile().getOriginalFilename());
        m.put("owner", r.getFromUser().getUsername());
        m.put("approvedAt", r.getRespondedAt() == null ? null : r.getRespondedAt().toString());
        return m;
      }).toList();

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
