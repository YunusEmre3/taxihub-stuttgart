package com.stuttgarttaxi.taxihub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catalog of optional extras offered on the public booking site. Source of
 * truth for pricing, same role ExtraService plays for PricingRule - the
 * public API and the confirmation email both read from here, never from a
 * hardcoded frontend price.
 */
@Entity
@Table(name = "extra_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ExtraServiceCode code;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Double price;
}
