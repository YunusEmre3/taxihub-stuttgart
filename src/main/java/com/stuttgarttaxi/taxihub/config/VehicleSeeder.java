package com.stuttgarttaxi.taxihub.config;

import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.entity.Vehicle;
import com.stuttgarttaxi.taxihub.entity.VehicleStatus;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.repository.EmployeeRepository;
import com.stuttgarttaxi.taxihub.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a small fleet on first startup, in the 4 tiers Booking already uses
 * (VehicleType), with a couple of real EMPLOYEE drivers assigned - no
 * fabricated driver names, and never an ADMIN as a "driver".
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class VehicleSeeder implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;

    private record SeedVehicle(String plate, String model, int year, VehicleType type,
                                String vin, VehicleStatus status, boolean assignDriver) {
    }

    @Override
    public void run(String... args) {
        if (vehicleRepository.count() > 0) {
            return;
        }

        List<Employee> drivers = employeeRepository.findAll().stream()
                .filter(e -> e.getRole() == Role.EMPLOYEE)
                .toList();

        List<SeedVehicle> seedVehicles = List.of(
                new SeedVehicle("S-TA 1001", "Mercedes E-Class", 2023, VehicleType.COMFORT,
                        "WDD2130421A123456", VehicleStatus.AVAILABLE, true),
                new SeedVehicle("S-TX 2044", "Volkswagen Passat", 2022, VehicleType.STANDARD,
                        "WVWZZZ3CZME123457", VehicleStatus.AVAILABLE, true),
                new SeedVehicle("S-KL 3312", "Mercedes V-Class", 2021, VehicleType.VAN,
                        "WDF4471331V123458", VehicleStatus.AVAILABLE, true),
                new SeedVehicle("S-RT 4590", "Mercedes S-Class", 2023, VehicleType.BUSINESS,
                        "WDD2221961A123459", VehicleStatus.AVAILABLE, false),
                new SeedVehicle("S-BW 5521", "Škoda Octavia", 2022, VehicleType.STANDARD,
                        "TMBJJ7NE0N0123460", VehicleStatus.MAINTENANCE, false),
                new SeedVehicle("S-FM 6633", "BMW 5 Series", 2023, VehicleType.COMFORT,
                        "WBA5A7C50FD123461", VehicleStatus.AVAILABLE, false),
                new SeedVehicle("S-HG 7744", "Volkswagen Multivan", 2021, VehicleType.VAN,
                        "WV2ZZZ7HZMH123462", VehicleStatus.AVAILABLE, false),
                new SeedVehicle("S-MK 8855", "BMW 7 Series", 2022, VehicleType.BUSINESS,
                        "WBA7E2C50FG123463", VehicleStatus.AVAILABLE, false),
                new SeedVehicle("S-PL 9966", "Škoda Superb", 2020, VehicleType.STANDARD,
                        "TMBAG7NP0L0123464", VehicleStatus.MAINTENANCE, false)
        );

        int driverIndex = 0;
        for (SeedVehicle seed : seedVehicles) {
            Vehicle.VehicleBuilder builder = Vehicle.builder()
                    .plateNumber(seed.plate())
                    .model(seed.model())
                    .year(seed.year())
                    .vehicleType(seed.type())
                    .vin(seed.vin())
                    .status(seed.status());

            if (seed.assignDriver() && driverIndex < drivers.size()) {
                builder.assignedDriver(drivers.get(driverIndex));
                driverIndex++;
            }

            vehicleRepository.save(builder.build());
        }

        log.info("{} araç filoya eklendi.", seedVehicles.size());
    }
}
