package com.securefiles.secure_file_transfer.service;

import com.securefiles.secure_file_transfer.model.ActivityLog;
import com.securefiles.secure_file_transfer.model.User;
import com.securefiles.secure_file_transfer.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {

  private final ActivityLogRepository activityRepo;

  public ActivityService(ActivityLogRepository activityRepo) {
    this.activityRepo = activityRepo;
  }

  public void log(User user, String actionType, Long fileId, String filename, String message) {
    if (user == null) return;

    ActivityLog log = new ActivityLog();
    log.setUser(user);
    log.setActionType(actionType);
    log.setFileId(fileId);
    log.setFilename(filename);
    log.setMessage(message);

    activityRepo.save(log);
  }
}