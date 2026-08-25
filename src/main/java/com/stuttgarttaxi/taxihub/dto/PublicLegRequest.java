package com.stuttgarttaxi.taxihub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One leg (outbound or return) of a public booking submission. Minibar
 * quantities are per-leg on purpose - a customer may want a drink only at
 * pickup, or only on the way back. Child seat counts live on
 * PublicBookingRequest instead, since they're free equipment that applies
 * to the whole booking, not a per-leg choice.
 */
@Getter
@Setter
public class PublicLegRequest {

    @NotBlank(message = "{validation.pickupAddress.required}")
    private String pickupAddress;

    @NotBlank(message = "{validation.dropoffAddress.required}")
    private String dropoffAddress;

    @NotNull(message = "{validation.date.required}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "{validation.time.required}")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime time;

    @PositiveOrZero(message = "{validation.quantity.invalid}")
    private int waterQty;

    @PositiveOrZero(message = "{validation.quantity.invalid}")
    private int colaQty;

    @PositiveOrZero(message = "{validation.quantity.invalid}")
    private int sodaLemonadeQty;

    @PositiveOrZero(message = "{validation.quantity.invalid}")
    private int orangeJuiceQty;
}
