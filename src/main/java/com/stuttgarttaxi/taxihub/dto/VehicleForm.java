package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.VehicleStatus;
import com.stuttgarttaxi.taxihub.entity.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleForm {

    @NotBlank(message = "{validation.plateNumber.required}")
    private String plateNumber;

    @NotBlank(message = "{validation.model.required}")
    private String model;

    @NotNull(message = "{validation.year.required}")
    @Min(value = 1990, message = "{validation.year.invalid}")
    @Max(value = 2100, message = "{validation.year.invalid}")
    private Integer year;

    @NotNull(message = "{validation.vehicleType.required}")
    private VehicleType vehicleType;

    @NotBlank(message = "{validation.vin.required}")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{11,17}$", message = "{validation.vin.invalid}")
    private String vin;

    @NotNull(message = "{validation.vehicleStatus.required}")
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    /**
     * Optional - only employees not already driving another vehicle are offered.
     */
    private Long assignedDriverId;
}
