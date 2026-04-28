package com.securefiles.secure_file_transfer.repository;

import com.securefiles.secure_file_transfer.model.ActivityLog;
import com.securefiles.secure_file_transfer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
   void deleteByUser(User user);
  List<ActivityLog> findTop10ByUserOrderByCreatedAtDesc(User user);
}