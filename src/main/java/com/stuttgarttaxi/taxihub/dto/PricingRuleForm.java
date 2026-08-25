package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PricingRuleForm {

    @NotNull
    private VehicleType vehicleType;

    @NotNull(message = "{validation.baseFare.required}")
    @Positive(message = "{validation.baseFare.invalid}")
    private Double baseFare;

    @NotNull(message = "{validation.pricePerKm.required}")
    @Positive(message = "{validation.pricePerKm.invalid}")
    private Double pricePerKm;
}
