package com.stuttgarttaxi.taxihub.controller;

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

/**
 * The live dispatch map: unassigned bookings and idle drivers on one screen,
 * with drag-and-drop (or click-to-assign) to pair them up. Admin-only - this
 * is a dispatcher tool, not something a driver uses on themselves.
 */
@Controller
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverDispatchController {

    private final BookingService bookingService;

    @GetMapping
    public String dispatchMap(Model model, @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        model.addAttribute("employeeName", userDetails.getEmployee().getFirstName());
        model.addAttribute("pendingBookings", bookingService.getPendingBookings());
        model.addAttribute("availableDrivers", bookingService.getAvailableEmployeesForAssignment());
        return "drivers/dispatch";
    }

    @PostMapping("/bookings/{bookingId}/assign/{employeeId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> assign(@PathVariable Long bookingId, @PathVariable Long employeeId) {
        try {
            bookingService.assignToEmployee(bookingId, employeeId);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (BookingConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (BookingNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }
}
