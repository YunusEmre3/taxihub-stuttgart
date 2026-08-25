package com.stuttgarttaxi.taxihub.repository;

import com.stuttgarttaxi.taxihub.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Employee> findByResetToken(String resetToken);
}
