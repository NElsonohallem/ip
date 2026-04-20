package com.securefiles.secure_file_transfer.controller;

import com.securefiles.secure_file_transfer.dto.ActivityItemResponse;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.ActivityLogRepository;
import com.securefiles.secure_file_transfer.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

  private final ActivityLogRepository activityRepo;
  private final UserRepository userRepo;

  public ActivityController(ActivityLogRepository activityRepo, UserRepository userRepo) {
    this.activityRepo = activityRepo;
    this.userRepo = userRepo;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalArgumentException("Not authenticated");
    }
    return auth.getName();
  }

  @GetMapping("/recent")
  public ResponseEntity<List<ActivityItemResponse>> recent() {
    String username = currentUsername();

    User user = userRepo.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

    List<ActivityItemResponse> result = activityRepo.findTop10ByUserOrderByCreatedAtDesc(user)
        .stream()
        .map(a -> new ActivityItemResponse(
            a.getId(),
            a.getActionType(),
            a.getFilename(),
            a.getFileId(),
            a.getMessage(),
            a.getCreatedAt()
        ))
        .toList();

    return ResponseEntity.ok(result);
  }
}