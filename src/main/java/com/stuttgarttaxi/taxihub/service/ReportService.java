package com.stuttgarttaxi.taxihub.service;

import com.stuttgarttaxi.taxihub.dto.ReportData;
import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.BookingStatus;
import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Every number here is counted from real Booking rows - there's no revenue
 * figure because Booking never persisted a price (see BookingCreateForm /
 * RouteService: the estimated price is shown to the dispatcher when
 * creating a booking, but it was a deliberate choice not to store it).
 * Faking a currency figure here would be worse than not having one.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", new Locale("en"));
    private static final List<String> HOUR_BUCKET_LABELS =
            List.of("00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00");

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public ReportData getReportData(LocalDate dateFrom, LocalDate dateTo, VehicleType vehicleTypeFilter) {
        // assignedEmployee needs to come back eagerly fetched (see the
        // @EntityGraph on this method) - Booking.assignedEmployee is lazy,
        // and grouping by driver below would otherwise throw once the
        // transaction that loaded these rows has closed.
        List<Booking> filtered = bookingRepository.findAllByOrderByBookingDateDescBookingTimeDesc().stream()
                .filter(b -> !b.getBookingDate().isBefore(dateFrom) && !b.getBookingDate().isAfter(dateTo))
                .filter(b -> vehicleTypeFilter == null || b.getVehicleType() == vehicleTypeFilter)
                .toList();

        return new ReportData(
                monthlyLabels(dateFrom, dateTo),
                monthlyVolume(filtered, dateFrom, dateTo),
                HOUR_BUCKET_LABELS,
                hourlyVolume(filtered),
                vehicleTypeLabels(),
                vehicleTypeCounts(filtered),
                topDriverNames(filtered),
                topDriverCompletedRides(filtered),
                averagePassengersPerRide(filtered),
                completionRatePercent(filtered),
                activeDriverCount(filtered)
        );
    }

    /**
     * Same filters as getReportData, but the raw rows - used for the CSV export.
     */
    @Transactional(readOnly = true)
    public List<Booking> getFilteredBookings(LocalDate dateFrom, LocalDate dateTo, VehicleType vehicleTypeFilter) {
        return bookingRepository.findAllByOrderByBookingDateDescBookingTimeDesc().stream()
                .filter(b -> !b.getBookingDate().isBefore(dateFrom) && !b.getBookingDate().isAfter(dateTo))
                .filter(b -> vehicleTypeFilter == null || b.getVehicleType() == vehicleTypeFilter)
                .toList();
    }

    private List<YearMonth> monthRange(LocalDate dateFrom, LocalDate dateTo) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = YearMonth.from(dateFrom);
        YearMonth end = YearMonth.from(dateTo);
        while (!cursor.isAfter(end)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    private List<String> monthlyLabels(LocalDate dateFrom, LocalDate dateTo) {
        return monthRange(dateFrom, dateTo).stream().map(ym -> ym.format(MONTH_LABEL)).toList();
    }

    private List<Long> monthlyVolume(List<Booking> bookings, LocalDate dateFrom, LocalDate dateTo) {
        Map<YearMonth, Long> countByMonth = bookings.stream()
                .collect(Collectors.groupingBy(b -> YearMonth.from(b.getBookingDate()), TreeMap::new, Collectors.counting()));

        return monthRange(dateFrom, dateTo).stream()
                .map(ym -> countByMonth.getOrDefault(ym, 0L))
                .toList();
    }

    private List<Long> hourlyVolume(List<Booking> bookings) {
        long[] buckets = new long[HOUR_BUCKET_LABELS.size()];
        for (Booking booking : bookings) {
            int bucketIndex = booking.getBookingTime().getHour() / 3;
            buckets[bucketIndex]++;
        }
        return java.util.Arrays.stream(buckets).boxed().toList();
    }

    private List<String> vehicleTypeLabels() {
        return java.util.Arrays.stream(VehicleType.values()).map(Enum::name).toList();
    }

    private List<Long> vehicleTypeCounts(List<Booking> bookings) {
        Map<VehicleType, Long> countByType = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getVehicleType, Collectors.counting()));

        return java.util.Arrays.stream(VehicleType.values())
                .map(type -> countByType.getOrDefault(type, 0L))
                .toList();
    }

    private List<Map.Entry<String, Long>> topDriverEntries(List<Booking> bookings) {
        Map<Long, Long> completedCountByDriverId = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getAssignedEmployee() != null)
                .collect(Collectors.groupingBy(b -> b.getAssignedEmployee().getId(), Collectors.counting()));

        Map<Long, String> nameByDriverId = bookings.stream()
                .filter(b -> b.getAssignedEmployee() != null)
                .collect(Collectors.toMap(
                        b -> b.getAssignedEmployee().getId(),
                        b -> nameOf(b.getAssignedEmployee()),
                        (a, ignoredDuplicate) -> a));

        return completedCountByDriverId.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> Map.entry(nameByDriverId.getOrDefault(entry.getKey(), "Bilinmeyen"), entry.getValue()))
                .toList();
    }

    private List<String> topDriverNames(List<Booking> bookings) {
        return topDriverEntries(bookings).stream().map(Map.Entry::getKey).toList();
    }

    private List<Long> topDriverCompletedRides(List<Booking> bookings) {
        return topDriverEntries(bookings).stream().map(Map.Entry::getValue).toList();
    }

    private double averagePassengersPerRide(List<Booking> bookings) {
        return bookings.stream()
                .mapToInt(Booking::getPassengerCount)
                .average()
                .orElse(0);
    }

    private double completionRatePercent(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return 0;
        }
        long completed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        return (completed * 100.0) / bookings.size();
    }

    private long activeDriverCount(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getAssignedEmployee() != null)
                .map(b -> b.getAssignedEmployee().getId())
                .distinct()
                .count();
    }

    private String nameOf(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }
}
