package com.stuttgarttaxi.taxihub.service;

import com.stuttgarttaxi.taxihub.dto.PricingRuleForm;
import com.stuttgarttaxi.taxihub.entity.PricingRule;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The single source of truth for what a ride costs. RouteService reads
 * these rules to price every "Show Route" call - there's no separate
 * "display" price and "real" price, just this.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;

    @Transactional(readOnly = true)
    public List<PricingRule> getAllRules() {
        return pricingRuleRepository.findAllByOrderByVehicleTypeAsc();
    }

    /**
     * Falls back to STANDARD if a rule is somehow missing for the requested
     * type (shouldn't happen once PricingRuleSeeder has run, but a route
     * calculation should never hard-fail over a missing fare tier).
     */
    @Transactional(readOnly = true)
    public PricingRule getRuleFor(VehicleType vehicleType) {
        return pricingRuleRepository.findByVehicleType(vehicleType)
                .or(() -> pricingRuleRepository.findByVehicleType(VehicleType.STANDARD))
                .orElseThrow(() -> new IllegalStateException(
                        "Hiçbir fiyatlandırma kuralı tanımlı değil - PricingRuleSeeder çalışmamış olabilir"));
    }

    @Transactional
    public void updateRules(List<PricingRuleForm> forms) {
        for (PricingRuleForm form : forms) {
            PricingRule rule = pricingRuleRepository.findByVehicleType(form.getVehicleType())
                    .orElseGet(() -> PricingRule.builder().vehicleType(form.getVehicleType()).build());
            rule.setBaseFare(form.getBaseFare());
            rule.setPricePerKm(form.getPricePerKm());
            pricingRuleRepository.save(rule);
        }
    }
}
