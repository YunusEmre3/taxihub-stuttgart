package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.PaymentMethod;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Backs the "Create New Booking" form. Mirrors the public booking form
 * fields 1:1 (see Booking entity), plus the dispatcher-only driver/payment
 * selection that the public website form doesn't have.
 */
@Getter
@Setter
public class BookingCreateForm {

    // ---- Customer Information ----

    @NotBlank(message = "{validation.lastName.required}")
    private String lastName;

    @NotBlank(message = "{validation.firstName.required}")
    private String firstName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.phoneNumber.required}")
    @Pattern(regexp = "^[0-9+()\\-\\s]{6,20}$", message = "{validation.phoneNumber.invalid}")
    private String phoneNumber;

    private String customerMessage;

    // ---- Trip Details ----

    @NotNull(message = "{validation.date.required}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @NotNull(message = "{validation.time.required}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime bookingTime;

    @NotBlank(message = "{validation.pickupAddress.required}")
    private String pickupAddress;

    @NotBlank(message = "{validation.dropoffAddress.required}")
    private String dropoffAddress;

    /**
     * Filled in silently by booking-create.js after a successful "Show Route"
     * call, so the pickup point can be pinned on the dispatch map later
     * without geocoding the address a second time. Stays null if the admin
     * never triggered route calculation - that booking just won't get a pin.
     */
    private Double pickupLat;
    private Double pickupLng;

    @NotNull(message = "{validation.vehicleType.required}")
    private VehicleType vehicleType;

    @NotNull(message = "{validation.passengerCount.required}")
    @Positive(message = "{validation.passengerCount.invalid}")
    private Integer passengerCount;

    @NotNull(message = "{validation.luggageCount.required}")
    @Positive(message = "{validation.luggageCount.invalid}")
    private Integer luggageCount;

    // ---- Vehicle & Driver Selection ----

    /**
     * Optional - if set, the booking is created as ASSIGNED to this employee
     * instead of PENDING. Only employees free of an active job are offered.
     */
    private Long assignedEmployeeId;

    // ---- Payment Information ----

    private PaymentMethod paymentMethod;
}
