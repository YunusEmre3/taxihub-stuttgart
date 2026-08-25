package com.stuttgarttaxi.taxihub.repository;

import com.stuttgarttaxi.taxihub.entity.Vehicle;
import com.stuttgarttaxi.taxihub.entity.VehicleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlateNumberIgnoreCase(String plateNumber);

    boolean existsByVinIgnoreCase(String vin);

    @EntityGraph(attributePaths = "assignedDriver")
    List<Vehicle> findAllByOrderByPlateNumberAsc();

    @EntityGraph(attributePaths = "assignedDriver")
    Optional<Vehicle> findWithAssignedDriverById(Long id);

    long countByStatus(VehicleStatus status);

    long countByAssignedDriverIsNull();

    boolean existsByAssignedDriverId(Long employeeId);

    Optional<Vehicle> findByAssignedDriverId(Long employeeId);
}
