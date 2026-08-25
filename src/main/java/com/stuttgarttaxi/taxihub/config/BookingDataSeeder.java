package com.stuttgarttaxi.taxihub.config;

import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.BookingStatus;
import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.repository.BookingRepository;
import com.stuttgarttaxi.taxihub.repository.EmployeeRepository;
import com.stuttgarttaxi.taxihub.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Inserts a handful of realistic-looking bookings on first startup so the
 * admin/employee dashboards have something to show. Only runs once - it
 * no-ops if the bookings table already has data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingDataSeeder implements CommandLineRunner {

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final BookingService bookingService;

    private record SeedRow(
            String firstName, String lastName, String email, String phone,
            String pickup, String dropoff, int dayOffset, LocalTime time,
            VehicleType vehicleType, int passengers, int luggage, String message,
            BookingStatus status, boolean assign, int hoursSinceAssigned, int hoursSinceCompleted
    ) {
    }

    @Override
    public void run(String... args) {
        if (bookingRepository.count() > 0) {
            log.info("Bookings tablosunda zaten veri var, seed atlanıyor.");
            return;
        }

        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(e -> e.getRole() == Role.EMPLOYEE)
                .toList();

        if (employees.isEmpty()) {
            log.warn("Atanabilecek EMPLOYEE bulunamadı; tüm örnek rezervasyonlar PENDING olarak eklenecek.");
        }

        List<SeedRow> rows = List.of(
                // ---- PENDING (henüz kimseye atanmamış) ----
                new SeedRow("Michael", "Bauer", "michael.bauer@web.de", "0151 22334455",
                        "Stuttgart Hauptbahnhof, Arnulf-Klett-Platz 2", "Flughafen Stuttgart (STR), Terminal 1",
                        0, LocalTime.of(14, 30), VehicleType.BUSINESS, 2, 3,
                        "Uçağımız 15:45'te kalkıyor, lütfen zamanında olun.", BookingStatus.PENDING, false, 0, 0),
                new SeedRow("Sabine", "Hofmann", "sabine.hofmann@gmx.de", "0160 98765432",
                        "Königstraße 15, Stuttgart-Mitte", "Schlossplatz, Bad Cannstatt",
                        0, LocalTime.of(18, 0), VehicleType.STANDARD, 1, 1,
                        null, BookingStatus.PENDING, false, 0, 0),
                new SeedRow("Jonas", "Krüger", "jonas.krueger@t-online.de", "0170 11223344",
                        "Marienplatz 3, Stuttgart-Süd", "Mercedes-Benz Museum, Untertürkheim",
                        0, LocalTime.of(9, 15), VehicleType.COMFORT, 4, 2,
                        "4 kişiyiz, bagaj biraz büyük.", BookingStatus.PENDING, false, 0, 0),
                new SeedRow("Hannah", "Schulz", "hannah.schulz@gmail.com", "0176 55667788",
                        "Killesbergpark, Stuttgart-Nord", "Wilhelma Zoo, Bad Cannstatt",
                        1, LocalTime.of(11, 0), VehicleType.VAN, 6, 4,
                        "Ailece geziyoruz, çocuk koltuğu gerekiyor.", BookingStatus.PENDING, false, 0, 0),
                new SeedRow("Leon", "Wagner", "leon.wagner@web.de", "0151 99887766",
                        "Vaihingen Bahnhof", "Möhringen, Rathausplatz",
                        1, LocalTime.of(20, 30), VehicleType.STANDARD, 1, 0,
                        null, BookingStatus.PENDING, false, 0, 0),
                new SeedRow("Laura", "Neumann", "laura.neumann@gmx.de", "0163 44556677",
                        "Zuffenhausen, Porscheplatz", "Flughafen Stuttgart (STR), Terminal 3",
                        2, LocalTime.of(6, 45), VehicleType.BUSINESS, 1, 2,
                        "Erken uçuş, lütfen 6:45'te tam zamanında.", BookingStatus.PENDING, false, 0, 0),

                // ---- ASSIGNED (bir çalışana atanmış, henüz yolda değil) ----
                new SeedRow("Stefan", "Klein", "stefan.klein@gmail.com", "0170 22113344",
                        "Feuerbach, Wilhelmstraße 10", "Degerloch, Waldau",
                        0, LocalTime.of(13, 0), VehicleType.COMFORT, 2, 1,
                        null, BookingStatus.ASSIGNED, true, 1, 0),
                new SeedRow("Julia", "Fischer", "julia.fischer@web.de", "0151 33224455",
                        "Weilimdorf, Solitudestraße", "Stuttgart Hauptbahnhof",
                        0, LocalTime.of(16, 15), VehicleType.STANDARD, 1, 1,
                        null, BookingStatus.PENDING, false, 0, 0),
                new SeedRow("Maximilian", "Richter", "max.richter@gmx.de", "0176 88997766",
                        "Botnang, Rembrandtweg", "Stuttgart Flughafen (STR), Terminal 2",
                        1, LocalTime.of(7, 30), VehicleType.VAN, 5, 5,
                        "Kayak takımları var, bagajlıkta yer olmalı.", BookingStatus.PENDING, false, 0, 0),
                new SeedRow("Emma", "Zimmermann", "emma.zimmermann@t-online.de", "0160 33445566",
                        "Sillenbuch, Ruit", "Plieningen, Universität Hohenheim",
                        0, LocalTime.of(19, 45), VehicleType.STANDARD, 2, 0,
                        null, BookingStatus.PENDING, false, 0, 0),

                // ---- IN_PROGRESS (şu an yolda) ----
                new SeedRow("Felix", "Braun", "felix.braun@gmail.com", "0170 66778899",
                        "Bad Cannstatt, Wilhelmsplatz", "Stuttgart-Ost, Gänsheide",
                        0, LocalTime.of(12, 0), VehicleType.COMFORT, 2, 2,
                        null, BookingStatus.IN_PROGRESS, true, 1, 0),
                new SeedRow("Sophie", "Schwarz", "sophie.schwarz@web.de", "0151 77889900",
                        "Stuttgart-West, Rotebühlplatz", "Vaihingen, Universität Stuttgart",
                        0, LocalTime.of(8, 30), VehicleType.STANDARD, 1, 1,
                        "Derse yetişmem lazım, mümkünse hızlı.", BookingStatus.IN_PROGRESS, true, 1, 0),
                new SeedRow("Lukas", "Hoffmann", "lukas.hoffmann@gmx.de", "0163 11224455",
                        "Möhringen, SI-Centrum", "Flughafen Stuttgart (STR), Terminal 1",
                        0, LocalTime.of(15, 0), VehicleType.BUSINESS, 3, 3,
                        null, BookingStatus.PENDING, false, 0, 0),

                // ---- COMPLETED (tamamlanmış geçmiş işler) ----
                new SeedRow("Anna", "Koch", "anna.koch@gmail.com", "0170 44556677",
                        "Degerloch, Albstraße", "Stuttgart Hauptbahnhof",
                        -1, LocalTime.of(10, 0), VehicleType.STANDARD, 1, 1,
                        null, BookingStatus.COMPLETED, true, 26, 25),
                new SeedRow("Paul", "Weber", "paul.weber@web.de", "0151 55667788",
                        "Untertürkheim, NeckarPark", "Bad Cannstatt, Kursaal",
                        -1, LocalTime.of(17, 30), VehicleType.COMFORT, 2, 1,
                        null, BookingStatus.COMPLETED, true, 20, 19),
                new SeedRow("Mia", "Meyer", "mia.meyer@gmx.de", "0176 22334466",
                        "Stuttgart-Nord, Killesberg", "Zuffenhausen, Porscheplatz",
                        -2, LocalTime.of(9, 0), VehicleType.VAN, 5, 4,
                        "Taşınma eşyaları da olacak.", BookingStatus.COMPLETED, true, 44, 42),
                new SeedRow("Thomas", "Schneider", "thomas.schneider@t-online.de", "0160 99001122",
                        "Flughafen Stuttgart (STR), Terminal 3", "Stuttgart-Mitte, Schlossplatz",
                        -3, LocalTime.of(21, 0), VehicleType.BUSINESS, 2, 2,
                        null, BookingStatus.COMPLETED, true, 68, 67),

                // ---- CANCELLED ----
                new SeedRow("Andreas", "Wolf", "andreas.wolf@gmail.com", "0151 66778800",
                        "Plieningen, Schelmenwasen", "Möhringen, Rathausplatz",
                        0, LocalTime.of(22, 0), VehicleType.STANDARD, 1, 0,
                        "Planım değişti, iptal etmek istiyorum.", BookingStatus.CANCELLED, false, 0, 0)
        );

        // An employee can only have one unfinished (ASSIGNED/IN_PROGRESS) booking at a
        // time - the same rule BookingService.assignToSelf() enforces at runtime. Track
        // who's already "busy" so the seed data never violates that invariant.
        java.util.Set<Long> employeesWithActiveJob = new java.util.HashSet<>();
        int completedRoundRobinIndex = 0;

        for (SeedRow row : rows) {
            Booking.BookingBuilder builder = Booking.builder()
                    .bookingCode(bookingService.generateBookingCode())
                    .firstName(row.firstName())
                    .lastName(row.lastName())
                    .email(row.email())
                    .phoneNumber(row.phone())
                    .pickupAddress(row.pickup())
                    .dropoffAddress(row.dropoff())
                    .bookingDate(LocalDate.now().plusDays(row.dayOffset()))
                    .bookingTime(row.time())
                    .vehicleType(row.vehicleType())
                    .passengerCount(row.passengers())
                    .luggageCount(row.luggage())
                    .customerMessage(row.message())
                    .status(row.status());

            boolean isActiveStatus = row.status() == BookingStatus.ASSIGNED || row.status() == BookingStatus.IN_PROGRESS;
            boolean canAssign = row.assign() && !employees.isEmpty();

            if (canAssign && isActiveStatus) {
                Employee freeEmployee = employees.stream()
                        .filter(e -> !employeesWithActiveJob.contains(e.getId()))
                        .findFirst()
                        .orElse(null);

                if (freeEmployee != null) {
                    employeesWithActiveJob.add(freeEmployee.getId());
                    builder.assignedEmployee(freeEmployee)
                            .assignedAt(LocalDateTime.now().minusHours(row.hoursSinceAssigned()));
                } else {
                    // Every employee already has an active job in this seed set -
                    // this ride just hasn't been picked up yet.
                    builder.status(BookingStatus.PENDING);
                }
            } else if (canAssign) {
                // COMPLETED bookings don't hold an employee "busy", so they can be
                // handed out freely regardless of who currently has an active job.
                Employee employee = employees.get(completedRoundRobinIndex % employees.size());
                completedRoundRobinIndex++;

                builder.assignedEmployee(employee)
                        .assignedAt(LocalDateTime.now().minusHours(row.hoursSinceAssigned()))
                        .completedAt(LocalDateTime.now().minusHours(row.hoursSinceCompleted()));
            } else if (row.assign()) {
                // No employees to assign to yet - keep it consistent as PENDING.
                builder.status(BookingStatus.PENDING);
            }

            bookingRepository.save(builder.build());
        }

        log.info("{} örnek rezervasyon eklendi.", rows.size());
    }
}
