package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.Employee;
import com.stuttgarttaxi.taxihub.exception.BookingAccessDeniedException;
import com.stuttgarttaxi.taxihub.exception.BookingConflictException;
import com.stuttgarttaxi.taxihub.exception.BookingNotFoundException;
import com.stuttgarttaxi.taxihub.service.BookingService;
import com.stuttgarttaxi.taxihub.service.EmployeeUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class EmployeeDashboardController {

    private final BookingService bookingService;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal EmployeeUserDetails userDetails, Model model) {
        Employee employee = userDetails.getEmployee();
        Optional<Booking> activeBooking = bookingService.getActiveBookingForEmployee(employee.getId());

        model.addAttribute("employeeName", employee.getFirstName());
        model.addAttribute("todayCompletedCount", bookingService.getTodayCompletedCountForEmployee(employee.getId()));
        model.addAttribute("activeBooking", activeBooking.orElse(null));

        // Only load the PENDING queue when the employee is free to take a new job -
        // this is also enforced server-side in assign(), not just hidden client-side.
        if (activeBooking.isEmpty()) {
            model.addAttribute("pendingBookings", bookingService.getPendingBookings());
        }

        return "employee/dashboard";
    }

    @PostMapping("/bookings/{id}/assign")
    @ResponseBody
    public ResponseEntity<Map<String, String>> assignToSelf(@PathVariable Long id,
                                                              @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        try {
            bookingService.assignToSelf(id, userDetails.getEmployee());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (BookingConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (BookingNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/bookings/{id}/complete")
    @ResponseBody
    public ResponseEntity<Map<String, String>> completeTrip(@PathVariable Long id,
                                                              @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        try {
            bookingService.completeTrip(id, userDetails.getEmployee());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (BookingAccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        } catch (BookingConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (BookingNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }
}
