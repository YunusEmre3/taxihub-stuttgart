package com.stuttgarttaxi.taxihub.config;

import com.stuttgarttaxi.taxihub.entity.PricingRule;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import com.stuttgarttaxi.taxihub.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds one fare tier per VehicleType on first startup. STANDARD keeps the
 * project's original flat rate (5.00 base / 1.50 per km) so existing
 * behavior doesn't change; the other tiers are priced up from there, the
 * way a real taxi company tiers COMFORT/VAN/BUSINESS above its base fare.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class PricingRuleSeeder implements CommandLineRunner {

    private final PricingRuleRepository pricingRuleRepository;

    @Override
    public void run(String... args) {
        if (pricingRuleRepository.count() > 0) {
            return;
        }

        pricingRuleRepository.save(PricingRule.builder().vehicleType(VehicleType.STANDARD).baseFare(5.00).pricePerKm(1.50).build());
        pricingRuleRepository.save(PricingRule.builder().vehicleType(VehicleType.COMFORT).baseFare(7.00).pricePerKm(1.80).build());
        pricingRuleRepository.save(PricingRule.builder().vehicleType(VehicleType.VAN).baseFare(8.00).pricePerKm(2.00).build());
        pricingRuleRepository.save(PricingRule.builder().vehicleType(VehicleType.BUSINESS).baseFare(12.00).pricePerKm(2.50).build());

        log.info("4 araç tipi için fiyatlandırma kuralı oluşturuldu.");
    }
}
