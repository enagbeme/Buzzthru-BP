package com.example.timetracking.repo;

import com.example.timetracking.model.TimeEntryAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeEntryAuditRepository extends JpaRepository<TimeEntryAudit, Long> {
}
