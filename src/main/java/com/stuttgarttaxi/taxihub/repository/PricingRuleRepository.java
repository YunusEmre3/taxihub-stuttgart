package com.stuttgarttaxi.taxihub.repository;

import com.stuttgarttaxi.taxihub.entity.PricingRule;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    Optional<PricingRule> findByVehicleType(VehicleType vehicleType);

    List<PricingRule> findAllByOrderByVehicleTypeAsc();
}
