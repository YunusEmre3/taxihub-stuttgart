package com.stuttgarttaxi.taxihub.controller;

import com.stuttgarttaxi.taxihub.dto.*;
import com.stuttgarttaxi.taxihub.entity.Booking;
import com.stuttgarttaxi.taxihub.entity.ExtraServiceCode;
import com.stuttgarttaxi.taxihub.entity.LegType;
import com.stuttgarttaxi.taxihub.entity.PricingRule;
import com.stuttgarttaxi.taxihub.repository.ExtraServiceRepository;
import com.stuttgarttaxi.taxihub.service.BookingService;
import com.stuttgarttaxi.taxihub.service.PricingService;
import com.stuttgarttaxi.taxihub.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unauthenticated API backing the public booking site (frontend/). Pricing
 * is always computed server-side from PricingService/ExtraServiceRepository
 * - the client never gets to dictate what a ride costs, same rule
 * RouteService already enforces for the internal admin booking form.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicBookingController {

    private static final String CURRENCY = "EUR";

    private final BookingService bookingService;
    private final RouteService routeService;
    private final PricingService pricingService;
    private final ExtraServiceRepository extraServiceRepository;

    @Value("${contact.phone}")
    private String contactPhone;

    @Value("${contact.whatsapp-number}")
    private String contactWhatsappNumber;

    @Value("${contact.email}")
    private String contactEmail;

    @Value("${contact.address}")
    private String contactAddress;

    @GetMapping("/contact")
    public ContactInfo contact() {
        return new ContactInfo(contactPhone, contactWhatsappNumber, contactEmail, contactAddress);
    }

    @GetMapping("/pricing")
    public PublicPricingResponse pricing() {
        List<VehiclePricingRow> vehicleTypes = pricingService.getAllRules().stream()
                .map(rule -> new VehiclePricingRow(rule.getVehicleType(), rule.getBaseFare(), rule.getPricePerKm()))
                .toList();

        List<ExtraServiceRow> extras = extraServiceRepository.findAllByOrderByIdAsc().stream()
                .map(extra -> new ExtraServiceRow(extra.getCode(), extra.getLabel(), extra.getPrice()))
                .toList();

        return new PublicPricingResponse(vehicleTypes, extras, CURRENCY);
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@Valid @RequestBody PublicBookingRequest request) {
        if (request.getTripType() == TripType.ROUND_TRIP && request.getReturnLeg() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Gidiş-dönüş seyahat için dönüş bilgileri zorunludur"));
        }

        PricingRule pricingRule = pricingService.getRuleFor(request.getVehicleType());
        double childSeatExtras = childSeatPrice(request);

        String groupId = request.getTripType() == TripType.ROUND_TRIP ? UUID.randomUUID().toString() : null;
        LegType outboundLegType = request.getTripType() == TripType.ROUND_TRIP ? LegType.OUTBOUND : null;

        List<PublicBookingLegResult> legResults = new ArrayList<>();
        double total = 0.0;

        RouteResult outboundRoute = routeService.calculateRoute(
                request.getOutboundLeg().getPickupAddress(), request.getOutboundLeg().getDropoffAddress(), request.getVehicleType());
        double outboundPrice = (outboundRoute.success() ? outboundRoute.estimatedPrice() : pricingRule.getBaseFare())
                + childSeatExtras + minibarPrice(request.getOutboundLeg());
        total += outboundPrice;

        Booking outboundBooking = bookingService.createPublicBooking(
                request, request.getOutboundLeg(), groupId, outboundLegType,
                outboundRoute.success() ? outboundRoute.pickup().lat() : null,
                outboundRoute.success() ? outboundRoute.pickup().lon() : null);

        legResults.add(new PublicBookingLegResult(
                outboundBooking.getBookingCode(), outboundLegType,
                outboundBooking.getPickupAddress(), outboundBooking.getDropoffAddress(),
                outboundBooking.getBookingDate(), outboundBooking.getBookingTime(),
                round(outboundPrice)));

        if (request.getTripType() == TripType.ROUND_TRIP) {
            RouteResult returnRoute = routeService.calculateRoute(
                    request.getReturnLeg().getPickupAddress(), request.getReturnLeg().getDropoffAddress(), request.getVehicleType());
            double returnPrice = (returnRoute.success() ? returnRoute.estimatedPrice() : pricingRule.getBaseFare())
                    + childSeatExtras + minibarPrice(request.getReturnLeg());
            total += returnPrice;

            Booking returnBooking = bookingService.createPublicBooking(
                    request, request.getReturnLeg(), groupId, LegType.RETURN,
                    returnRoute.success() ? returnRoute.pickup().lat() : null,
                    returnRoute.success() ? returnRoute.pickup().lon() : null);

            legResults.add(new PublicBookingLegResult(
                    returnBooking.getBookingCode(), LegType.RETURN,
                    returnBooking.getPickupAddress(), returnBooking.getDropoffAddress(),
                    returnBooking.getBookingDate(), returnBooking.getBookingTime(),
                    round(returnPrice)));
        }

        return ResponseEntity.ok(new PublicBookingResponse(legResults, round(total), CURRENCY));
    }

    /** Free equipment - included here only so the same helper computes both legs' totals identically. */
    private double childSeatPrice(PublicBookingRequest request) {
        return request.getBabySeatCount() * priceOf(ExtraServiceCode.BABY_SEAT)
                + request.getChildSeatCount() * priceOf(ExtraServiceCode.CHILD_SEAT)
                + request.getBoosterSeatCount() * priceOf(ExtraServiceCode.BOOSTER_SEAT);
    }

    private double minibarPrice(PublicLegRequest leg) {
        return leg.getWaterQty() * priceOf(ExtraServiceCode.WATER)
                + leg.getColaQty() * priceOf(ExtraServiceCode.COLA)
                + leg.getSodaLemonadeQty() * priceOf(ExtraServiceCode.SODA_LEMONADE)
                + leg.getOrangeJuiceQty() * priceOf(ExtraServiceCode.ORANGE_JUICE);
    }

    private double priceOf(ExtraServiceCode code) {
        return extraServiceRepository.findByCode(code).map(extra -> extra.getPrice()).orElse(0.0);
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
