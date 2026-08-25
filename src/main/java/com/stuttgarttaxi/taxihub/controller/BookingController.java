package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.BookingCreateForm;
import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.PaymentMethod;
import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.exception.BookingConflictException;
import com.stuttgarttaxi.taxihub.exception.BookingNotFoundException;
import com.stuttgarttaxi.taxihub.service.BookingService;
import com.stuttgarttaxi.taxihub.service.EmployeeUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/bookings/new")
    public String newBookingForm(Model model, @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingCreateForm());
        }
        addFormReferenceData(model, userDetails);
        return "bookings/create";
    }

    @PostMapping("/bookings")
    public String createBooking(@Valid @ModelAttribute("bookingForm") BookingCreateForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, userDetails);
            return "bookings/create";
        }

        Booking created;
        try {
            created = bookingService.createBooking(form);
        } catch (BookingConflictException | BookingNotFoundException ex) {
            bindingResult.rejectValue("assignedEmployeeId", "employee.unavailable", ex.getMessage());
            addFormReferenceData(model, userDetails);
            return "bookings/create";
        }

        // Redirects to the tracking page (instead of the dashboard) so the
        // dispatcher immediately sees the PNR with a copy button - same
        // "show the code right after creation" UX the public site's
        // success screen gives customers.
        redirectAttributes.addFlashAttribute("justCreated", true);
        return "redirect:/bookings/" + created.getId();
    }

    @GetMapping("/bookings/{id}")
    public String trackBooking(@PathVariable Long id, Model model, @AuthenticationPrincipal EmployeeUserDetails userDetails) {
        Booking booking;
        try {
            booking = bookingService.getBookingById(id);
        } catch (BookingNotFoundException ex) {
            return "redirect:" + dashboardUrlFor(userDetails);
        }

        model.addAttribute("booking", booking);
        model.addAttribute("employeeName", userDetails.getEmployee().getFirstName());
        model.addAttribute("roleLabel", userDetails.getEmployee().getRole().name());
        model.addAttribute("dashboardUrl", dashboardUrlFor(userDetails));
        return "bookings/track";
    }

    private void addFormReferenceData(Model model, EmployeeUserDetails userDetails) {
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("availableEmployees", bookingService.getAvailableEmployeesForAssignment());
        model.addAttribute("employeeName", userDetails.getEmployee().getFirstName());
        model.addAttribute("roleLabel", userDetails.getEmployee().getRole().name());
        model.addAttribute("dashboardUrl", dashboardUrlFor(userDetails));
    }

    private String dashboardUrlFor(EmployeeUserDetails userDetails) {
        return userDetails.getEmployee().getRole() == Role.ADMIN ? "/admin/dashboard" : "/dashboard";
    }
}
