package com.stuttgarttaxi.taxihub.service;

import com.stuttgarttaxi.taxihub.dto.VehicleForm;
import com.stuttgarttaxi.taxihub.dto.VehicleKpis;
import com.stuttgarttaxi.taxihub.dto.VehicleRow;
import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.entity.Vehicle;
import com.stuttgarttaxi.taxihub.entity.VehicleStatus;
import com.stuttgarttaxi.taxihub.exception.VehicleConflictException;
import com.stuttgarttaxi.taxihub.exception.VehicleNotFoundException;
import com.stuttgarttaxi.taxihub.repository.EmployeeRepository;
import com.stuttgarttaxi.taxihub.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final BookingService bookingService;

    @Transactional(readOnly = true)
    public List<VehicleRow> getAllVehicles() {
        return vehicleRepository.findAllByOrderByPlateNumberAsc().stream()
                .map(this::toRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleKpis getVehicleKpis() {
        long total = vehicleRepository.count();
        long maintenance = vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE);
        long unassigned = vehicleRepository.countByAssignedDriverIsNull();

        long inService = vehicleRepository.findAllByOrderByPlateNumberAsc().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                .filter(this::hasDriverCurrentlyOnATrip)
                .count();

        return new VehicleKpis(total, inService, maintenance, unassigned);
    }

    /**
     * Employees not already the assigned driver of some other vehicle - the
     * pool offered in the "Assigned Driver" dropdown. Only real drivers
     * (EMPLOYEE role) are eligible, never admins.
     */
    @Transactional(readOnly = true)
    public List<Employee> getDriversWithoutVehicle(Long excludingVehicleId) {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getRole() == Role.EMPLOYEE)
                .filter(e -> vehicleRepository.findByAssignedDriverId(e.getId())
                        .map(Vehicle::getId)
                        .map(assignedVehicleId -> assignedVehicleId.equals(excludingVehicleId))
                        .orElse(true))
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleRow getVehicleRowById(Long id) {
        return toRow(getVehicleById(id));
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findWithAssignedDriverById(id)
                .orElseThrow(() -> new VehicleNotFoundException("Araç bulunamadı"));
    }

    @Transactional
    public Vehicle createVehicle(VehicleForm form) {
        validateUnique(form, null);

        Vehicle.VehicleBuilder builder = Vehicle.builder()
                .plateNumber(form.getPlateNumber().trim())
                .model(form.getModel().trim())
                .year(form.getYear())
                .vehicleType(form.getVehicleType())
                .vin(form.getVin().trim().toUpperCase())
                .status(form.getStatus());

        if (form.getAssignedDriverId() != null) {
            builder.assignedDriver(resolveDriver(form.getAssignedDriverId(), null));
        }

        return vehicleRepository.save(builder.build());
    }

    @Transactional
    public Vehicle updateVehicle(Long id, VehicleForm form) {
        Vehicle vehicle = getVehicleById(id);
        validateUnique(form, id);

        vehicle.setPlateNumber(form.getPlateNumber().trim());
        vehicle.setModel(form.getModel().trim());
        vehicle.setYear(form.getYear());
        vehicle.setVehicleType(form.getVehicleType());
        vehicle.setVin(form.getVin().trim().toUpperCase());
        vehicle.setStatus(form.getStatus());
        vehicle.setAssignedDriver(form.getAssignedDriverId() != null
                ? resolveDriver(form.getAssignedDriverId(), id)
                : null);

        return vehicleRepository.save(vehicle);
    }

    private Employee resolveDriver(Long employeeId, Long excludingVehicleId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new VehicleConflictException("Seçilen sürücü bulunamadı"));

        boolean alreadyDrivesAnother = vehicleRepository.findByAssignedDriverId(employeeId)
                .map(Vehicle::getId)
                .map(assignedVehicleId -> !assignedVehicleId.equals(excludingVehicleId))
                .orElse(false);

        if (alreadyDrivesAnother) {
            throw new VehicleConflictException("Bu sürücü zaten başka bir araca atanmış");
        }

        return employee;
    }

    private void validateUnique(VehicleForm form, Long excludingVehicleId) {
        vehicleRepository.findAllByOrderByPlateNumberAsc().stream()
                .filter(v -> !v.getId().equals(excludingVehicleId))
                .filter(v -> v.getPlateNumber().equalsIgnoreCase(form.getPlateNumber().trim()))
                .findAny()
                .ifPresent(v -> {
                    throw new VehicleConflictException("Bu plaka zaten kayıtlı");
                });

        vehicleRepository.findAllByOrderByPlateNumberAsc().stream()
                .filter(v -> !v.getId().equals(excludingVehicleId))
                .filter(v -> v.getVin().equalsIgnoreCase(form.getVin().trim()))
                .findAny()
                .ifPresent(v -> {
                    throw new VehicleConflictException("Bu VIN zaten kayıtlı");
                });
    }

    private boolean hasDriverCurrentlyOnATrip(Vehicle vehicle) {
        Employee driver = vehicle.getAssignedDriver();
        return driver != null && bookingService.getActiveBookingForEmployee(driver.getId()).isPresent();
    }

    private VehicleRow toRow(Vehicle vehicle) {
        // A stable code (matched by the status filter dropdown and the
        // enum.vehicleDisplayStatus.* message keys), not a display string -
        // the template resolves it to the current language's label.
        String displayStatus;
        String cssClass;

        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            displayStatus = "MAINTENANCE";
            cssClass = "badge-maintenance";
        } else if (hasDriverCurrentlyOnATrip(vehicle)) {
            displayStatus = "IN_SERVICE";
            cssClass = "badge-in-service";
        } else {
            displayStatus = "AVAILABLE";
            cssClass = "badge-available";
        }

        Employee driver = vehicle.getAssignedDriver();

        return new VehicleRow(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getVehicleType(),
                vehicle.getVin(),
                driver != null ? driver.getId() : null,
                driver != null ? driver.getFirstName() + " " + driver.getLastName() : null,
                displayStatus,
                cssClass
        );
    }
}
