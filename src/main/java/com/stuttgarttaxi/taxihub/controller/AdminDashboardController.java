package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.service.BookingService;
import com.stuttgarttaxi.taxihub.service.EmployeeUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final BookingService bookingService;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal EmployeeUserDetails userDetails, Model model) {
        model.addAttribute("employeeName", userDetails.getEmployee().getFirstName());
        model.addAttribute("kpis", bookingService.getAdminKpis());
        model.addAttribute("bookings", bookingService.getAllBookingsForAdmin());
        return "admin/dashboard";
    }
}
