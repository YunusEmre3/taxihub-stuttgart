package com.stuttgarttaxi.taxihub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Mirrors the public booking form fields on stuttgart-taxi.com 1:1 so that
 * bookings created on the website can be inserted into this table as-is.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", nullable = false, unique = true, length = 20)
    private String bookingCode;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress;

    /**
     * Geocoded once, at creation time, from pickupAddress (best-effort - stays
     * null if geocoding failed or was never attempted). Lets the dispatch map
     * place a pin without re-geocoding on every page load.
     */
    @Column(name = "pickup_lat")
    private Double pickupLat;

    @Column(name = "pickup_lng")
    private Double pickupLng;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booking_time", nullable = false)
    private LocalTime bookingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "passenger_count", nullable = false)
    private Integer passengerCount;

    @Column(name = "luggage_count", nullable = false)
    private Integer luggageCount;

    @Column(name = "customer_message", columnDefinition = "TEXT")
    private String customerMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    /**
     * Extras selected on the public booking site. No price is stored here -
     * same rule as the rest of Booking (see estimatedPrice in RouteResult):
     * price is always computed live from ExtraService, never persisted.
     *
     * Child seats are free equipment, not a per-leg upsell: the same counts
     * are written to both legs of a round trip so the customer is never
     * asked twice. Minibar drinks are a real per-leg add-on (you might want
     * water on the way but not the way back), so they're per-leg like the
     * old water/meetAndGreet flags they replace.
     */
    @Column(name = "baby_seat_count")
    private Integer babySeatCount;

    @Column(name = "child_seat_count")
    private Integer childSeatCount;

    @Column(name = "booster_seat_count")
    private Integer boosterSeatCount;

    @Column(name = "water_qty")
    private Integer waterQty;

    @Column(name = "cola_qty")
    private Integer colaQty;

    @Column(name = "soda_lemonade_qty")
    private Integer sodaLemonadeQty;

    @Column(name = "orange_juice_qty")
    private Integer orangeJuiceQty;

    /**
     * Round trips from the public site are stored as two independent Booking
     * rows (own status, own driver assignment, own tracking) sharing one
     * groupId. Null for one-way and all admin-created bookings - dispatch,
     * reports and tracking need no changes since they already work per-row.
     */
    @Column(name = "group_id", length = 36)
    private String groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leg_type")
    private LegType legType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optimistic lock: guarantees that when two employees hit "Assign to Me"
     * on the same booking at the same time, only the first commit succeeds -
     * the second fails with an ObjectOptimisticLockingFailureException that
     * BookingService translates into a 409 conflict.
     */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
