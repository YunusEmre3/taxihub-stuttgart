package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.PaymentMethod;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of POST /api/public/bookings - the customer booking wizard's submit
 * payload. Round trips send both outboundLeg and returnLeg; each becomes
 * its own Booking row sharing one groupId (see BookingService.createPublicBooking).
 */
@Getter
@Setter
public class PublicBookingRequest {

    @NotNull(message = "{validation.tripType.required}")
    private TripType tripType;

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

    @NotNull(message = "{validation.vehicleType.required}")
    private VehicleType vehicleType;

    @NotNull(message = "{validation.passengerCount.required}")
    @Positive(message = "{validation.passengerCount.invalid}")
    private Integer passengerCount;

    @NotNull(message = "{validation.luggageCount.required}")
    @Positive(message = "{validation.luggageCount.invalid}")
    private Integer luggageCount;

    private PaymentMethod paymentMethod;

    /** Free equipment, not an upsell - same counts apply to both legs. */
    @PositiveOrZero(message = "{validation.quantity.invalid}")
    private int babySeatCount;

    @PositiveOrZero(message = "{validation.quantity.invalid}")
    private int childSeatCount;

    @PositiveOrZero(message = "{validation.quantity.invalid}")
    private int boosterSeatCount;

    @NotNull(message = "{validation.outboundLeg.required}")
    @Valid
    private PublicLegRequest outboundLeg;

    /** Required only when tripType == ROUND_TRIP. */
    @Valid
    private PublicLegRequest returnLeg;
}
