package com.graphsearch.FlightGraph.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

/**
 * Body of POST /flights: only what the client is allowed to send (no id — the server generates it).
 *
 * The annotations are FORMAT rules, checked by Bean Validation before the controller runs
 * (triggered by {@code @Valid}). Rules that need data (does the airport exist?) live in FlightService.
 */
public record CreateFlightRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter IATA airport code")
        String origin,

        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter IATA airport code")
        String destination,

        @NotNull OffsetDateTime departureTime,

        @NotNull OffsetDateTime arrivalTime,

        @Positive Double price, // optional: null means "no price known"

        @NotBlank @Pattern(regexp = "[A-Z0-9]{2}", message = "must be a 2-character IATA airline code")
        String carrier,

        @NotBlank @Pattern(regexp = "\\d{1,4}", message = "must be 1 to 4 digits")
        String flightNumber) {

    // Cross-field rules: Hibernate Validator treats boolean "isXxx()" methods as properties,
    // so @AssertTrue on them works like a field constraint (the error is reported under "xxx").

    @AssertTrue(message = "arrivalTime must be after departureTime")
    public boolean isArrivalAfterDeparture() {
        // null is already reported by @NotNull; don't report it twice
        return departureTime == null || arrivalTime == null || arrivalTime.isAfter(departureTime);
    }

    @AssertTrue(message = "origin and destination must be different")
    public boolean isDifferentAirports() {
        return origin == null || !origin.equals(destination);
    }
}
