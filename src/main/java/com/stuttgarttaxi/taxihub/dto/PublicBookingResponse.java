package com.stuttgarttaxi.taxihub.dto;

import java.util.List;

/** currency is the ISO 4217 code (EUR) - the € symbol stays in the UI, the code travels with the data. */
public record PublicBookingResponse(
        List<PublicBookingLegResult> legs,
        Double totalEstimatedPrice,
        String currency
) {
}
