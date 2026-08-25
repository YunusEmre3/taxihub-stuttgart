package com.stuttgarttaxi.taxihub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One fare tier per VehicleType. This is what RouteService actually reads
 * when it prices a route - editing these on the Settings page immediately
 * changes what "Show Route" quotes, not just a display-only table.
 */
@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, unique = true)
    private VehicleType vehicleType;

    @Column(name = "base_fare", nullable = false)
    private Double baseFare;

    @Column(name = "price_per_km", nullable = false)
    private Double pricePerKm;
}
