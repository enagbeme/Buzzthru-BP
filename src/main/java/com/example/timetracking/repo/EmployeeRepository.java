package com.example.timetracking.repo;

import com.example.timetracking.model.Employee;
import com.example.timetracking.model.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findAllByRoleAndActiveIsTrue(EmployeeRole role);
}
