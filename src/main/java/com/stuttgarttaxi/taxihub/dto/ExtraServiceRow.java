package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.ExtraServiceCode;

public record ExtraServiceRow(
        ExtraServiceCode code,
        String label,
        Double price
) {
}
