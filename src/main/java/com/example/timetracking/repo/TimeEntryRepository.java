package com.example.timetracking.repo;

import com.example.timetracking.model.Employee;
import com.example.timetracking.model.Location;
import com.example.timetracking.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    Optional<TimeEntry> findFirstByEmployeeAndClockOutTimeIsNull(Employee employee);

    @Query("select t from TimeEntry t where t.location = :location and t.clockOutTime is null")
    List<TimeEntry> findOpenByLocation(Location location);

    @Query("select t from TimeEntry t join fetch t.employee where t.location = :location and t.clockOutTime is null order by t.clockInTime desc")
    List<TimeEntry> findOpenByLocationWithEmployee(@Param("location") Location location);

    @Query("select t from TimeEntry t join fetch t.employee where t.clockOutTime is not null and t.clockInTime < :to and t.clockOutTime > :from")
    List<TimeEntry> findCompletedOverlapping(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select t from TimeEntry t join fetch t.employee where t.clockInTime < :to and (t.clockOutTime is null or t.clockOutTime > :from) order by t.clockInTime desc")
    List<TimeEntry> findOverlappingWithEmployee(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select t from TimeEntry t join fetch t.employee join fetch t.location join fetch t.device where t.clockInTime >= :from and t.clockInTime < :to order by t.clockInTime desc")
    List<TimeEntry> findBetweenWithDetails(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select t from TimeEntry t join fetch t.employee join fetch t.location join fetch t.device where t.id = :id")
    Optional<TimeEntry> findByIdWithDetails(@Param("id") long id);

    @Query("select t from TimeEntry t where t.employee = :employee and t.clockInTime >= :from and t.clockInTime < :to")
    List<TimeEntry> findByEmployeeBetween(Employee employee, Instant from, Instant to);
}
