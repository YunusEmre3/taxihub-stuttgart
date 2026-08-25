package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.ReportData;
import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.service.EmployeeUserDetails;
import com.stuttgarttaxi.taxihub.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping("/reports")
    public String dashboard(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
                             @RequestParam(required = false) VehicleType vehicleType,
                             Model model,
                             @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.now().minusMonths(6);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();

        model.addAttribute("employeeName", userDetails.getEmployee().getFirstName());
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("dateFrom", from);
        model.addAttribute("dateTo", to);
        model.addAttribute("selectedVehicleType", vehicleType);
        return "reports/dashboard";
    }

    @GetMapping("/api/reports/data")
    @ResponseBody
    public ReportData data(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
                            @RequestParam(required = false) VehicleType vehicleType) {
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.now().minusMonths(6);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();
        return reportService.getReportData(from, to, vehicleType);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
                                          @RequestParam(required = false) VehicleType vehicleType) {
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.now().minusMonths(6);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();
        List<Booking> bookings = reportService.getFilteredBookings(from, to, vehicleType);

        String csv = toCsv(bookings);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookings-export.csv\"")
                .body(bytes);
    }

    private String toCsv(List<Booking> bookings) {
        StringBuilder csv = new StringBuilder();
        csv.append("Booking Code,Customer,Pickup,Dropoff,Date,Time,Vehicle Type,Status,Driver,Payment Method\n");

        for (Booking b : bookings) {
            String driver = b.getAssignedEmployee() != null
                    ? b.getAssignedEmployee().getFirstName() + " " + b.getAssignedEmployee().getLastName()
                    : "";
            csv.append(csvCell(b.getBookingCode())).append(',')
                    .append(csvCell(b.getFirstName() + " " + b.getLastName())).append(',')
                    .append(csvCell(b.getPickupAddress())).append(',')
                    .append(csvCell(b.getDropoffAddress())).append(',')
                    .append(csvCell(b.getBookingDate().format(CSV_DATE))).append(',')
                    .append(csvCell(b.getBookingTime().format(CSV_TIME))).append(',')
                    .append(csvCell(b.getVehicleType().name())).append(',')
                    .append(csvCell(b.getStatus().name())).append(',')
                    .append(csvCell(driver)).append(',')
                    .append(csvCell(b.getPaymentMethod() != null ? b.getPaymentMethod().name() : ""))
                    .append('\n');
        }

        return csv.toString();
    }

    private String csvCell(String value) {
        String escaped = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
