package com.example.timetracking.repo;

import com.example.timetracking.model.TimeEntryAudit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeEntryAuditRepository extends JpaRepository<TimeEntryAudit, Long> {

    @Query("select a from TimeEntryAudit a join fetch a.timeEntry t join fetch t.employee join fetch t.location join fetch t.device left join fetch a.editedBy order by a.editedAt desc")
    List<TimeEntryAudit> findLatestWithDetails();
}
