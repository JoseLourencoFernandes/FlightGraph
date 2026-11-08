package com.graphsearch.FlightGraph.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Body of POST /airports. Format rules only; uniqueness of the code is checked in AirportService.
 */
public record CreateAirportRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter IATA airport code")
        String code,

        @NotBlank String name,

        @NotBlank String city,

        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "must be a 2-letter ISO country code")
        String country,

        // Double (not double): with a primitive, a missing field silently becomes 0.0 —
        // a valid coordinate in the Gulf of Guinea — and @NotNull could never catch it.
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,

        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,

        @NotBlank String timeZoneId) {

    @AssertTrue(message = "must be a valid IANA time zone, e.g. Europe/Lisbon")
    public boolean isValidTimeZoneId() {
        if (timeZoneId == null || timeZoneId.isBlank()) {
            return true; // already reported by @NotBlank
        }
        try {
            ZoneId.of(timeZoneId);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
