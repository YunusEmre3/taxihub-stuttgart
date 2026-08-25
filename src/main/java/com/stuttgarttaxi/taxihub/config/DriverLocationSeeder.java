package com.stuttgarttaxi.taxihub.config;

import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Gives every EMPLOYEE a demo position around Stuttgart so the dispatch map
 * (/drivers) has car markers to show. There's no real GPS reporting yet -
 * see the TODO on Employee.currentLat/currentLng - so this just fills in
 * whichever employees don't have a location, on every startup. Idempotent:
 * never overwrites a location once set.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DriverLocationSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;

    // A handful of real Stuttgart-area points spread across different
    // districts, just so the demo map doesn't cluster every car together.
    private static final List<double[]> DEMO_POSITIONS = List.of(
            new double[]{48.7758, 9.1829}, // Stuttgart-Mitte
            new double[]{48.8030, 9.1702}, // Killesberg / Stuttgart-Nord
            new double[]{48.7856, 9.2120}, // Bad Cannstatt
            new double[]{48.7270, 9.1123}, // Vaihingen
            new double[]{48.7789, 9.1252}  // Botnang
    );

    @Override
    public void run(String... args) {
        List<Employee> employeesNeedingLocation = employeeRepository.findAll().stream()
                .filter(e -> e.getRole() == Role.EMPLOYEE)
                .filter(e -> e.getCurrentLat() == null || e.getCurrentLng() == null)
                .toList();

        if (employeesNeedingLocation.isEmpty()) {
            return;
        }

        for (int i = 0; i < employeesNeedingLocation.size(); i++) {
            double[] position = DEMO_POSITIONS.get(i % DEMO_POSITIONS.size());
            Employee employee = employeesNeedingLocation.get(i);
            employee.setCurrentLat(position[0]);
            employee.setCurrentLng(position[1]);
            employeeRepository.save(employee);
        }

        log.info("{} çalışana demo konum atandı.", employeesNeedingLocation.size());
    }
}
