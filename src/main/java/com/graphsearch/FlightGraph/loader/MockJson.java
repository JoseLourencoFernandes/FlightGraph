package com.graphsearch.FlightGraph.loader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTOs (Data Transfer Objects) that mirror EXACTLY the structure of the mock-data JSON files.
 *
 * Why not deserialize straight into Airport/Flight?
 * The external format (Lufthansa-API style: PascalCase, deeply nested, schedules as patterns) differs
 * from our internal model. Keeping them apart means that when the data source changes (another API,
 * a database...), only the loader changes — the rest of the app doesn't know where the data came from.
 *
 * Records: immutable data-only classes, no hand-written getters/setters.
 * {@code @JsonProperty} maps a JSON field name that differs from the Java name.
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} skips fields we don't care about.
 */
public final class MockJson {

    private MockJson() {}

    // ---------- airports.json ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AirportsFile(@JsonProperty("AirportResource") AirportResource airportResource) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AirportResource(@JsonProperty("Airports") Airports airports) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Airports(@JsonProperty("Airport") List<AirportJson> airport) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AirportJson(
            @JsonProperty("AirportCode") String airportCode,
            @JsonProperty("Position") Position position,
            @JsonProperty("CityCode") String cityCode,
            @JsonProperty("CountryCode") String countryCode,
            @JsonProperty("Names") Names names,
            @JsonProperty("TimeZoneId") String timeZoneId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Position(@JsonProperty("Coordinate") Coordinate coordinate) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Coordinate(@JsonProperty("Latitude") double latitude,
                             @JsonProperty("Longitude") double longitude) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Names(@JsonProperty("Name") List<Name> name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Name(@JsonProperty("@LanguageCode") String languageCode,
                       @JsonProperty("$") String value) {}

    // ---------- schedules.json ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SchedulesFile(List<Route> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(String origin, String destination,
                        @JsonProperty("ScheduleResource") ScheduleResource scheduleResource) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduleResource(@JsonProperty("Schedule") List<Schedule> schedule) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Schedule(@JsonProperty("TotalJourney") TotalJourney totalJourney,
                           @JsonProperty("Flight") ScheduledFlight flight) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TotalJourney(@JsonProperty("Duration") String duration) {} // ISO-8601, e.g. "PT1H10M"

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduledFlight(@JsonProperty("Departure") Endpoint departure,
                                  @JsonProperty("Arrival") Endpoint arrival,
                                  @JsonProperty("MarketingCarrier") MarketingCarrier marketingCarrier,
                                  @JsonProperty("Details") Details details) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Endpoint(@JsonProperty("AirportCode") String airportCode,
                           @JsonProperty("ScheduledTimeLocal") ScheduledTimeLocal scheduledTimeLocal) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScheduledTimeLocal(@JsonProperty("DateTime") String dateTime) {} // e.g. "2027-05-01T07:45"

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MarketingCarrier(@JsonProperty("AirlineID") String airlineId,
                                   @JsonProperty("FlightNumber") String flightNumber) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Details(@JsonProperty("DaysOfOperation") String daysOfOperation, // "12345" = Mon to Fri
                          @JsonProperty("DatePeriod") DatePeriod datePeriod) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DatePeriod(@JsonProperty("Effective") String effective,
                             @JsonProperty("Expiration") String expiration) {}

    // ---------- prices.json ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricesFile(List<PriceJson> prices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceJson(String carrier, String flightNumber, String departureDate,
                            double amount, String currency) {}
}
