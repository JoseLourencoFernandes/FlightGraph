package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.dto.CreateAirportRequest;
import com.graphsearch.FlightGraph.exception.AirportAlreadyExistsException;
import com.graphsearch.FlightGraph.exception.AirportNotFoundException;
import com.graphsearch.FlightGraph.model.Airport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests for AirportService (no Spring). Format rules (3-letter code, valid time zone...)
 * are NOT tested here: they live in CreateAirportRequest and are enforced by @Valid in the web layer.
 * The service only owns rules that need existing data — uniqueness of the code.
 */
class AirportServiceTest {

    private static final Airport LISBON =
            new Airport("LIS", "Lisbon", "LIS", "PT", 38.78, -9.13, "Europe/Lisbon");

    private AirportService airportService;

    @BeforeEach
    void setUp() {
        airportService = new AirportService();
        airportService.addAll(List.of(LISBON));
    }

    @Nested
    class Lookup {

        // Same test, several inputs: each value is reported as its own test run.
        @ParameterizedTest
        @ValueSource(strings = {"LIS", "lis", "Lis"})
        void shouldFindAirportIgnoringCase(String code) {
            assertThat(airportService.getAirportByCode(code)).isSameAs(LISBON);
        }

        @Test
        void shouldThrowWhenCodeDoesNotExist() {
            assertThatThrownBy(() -> airportService.getAirportByCode("XYZ"))
                    .isInstanceOf(AirportNotFoundException.class)
                    .hasMessageContaining("XYZ");
        }

        @Test
        void findByCodeShouldReturnEmptyInsteadOfThrowing() {
            // findByCode lets the caller decide what "missing" means (FlightService turns it into a 422)
            assertThat(airportService.findByCode("XYZ")).isEmpty();
            assertThat(airportService.findByCode("LIS")).containsSame(LISBON);
        }
    }

    @Nested
    class CreateAirport {

        @Test
        void shouldCreateAirportCopyingEveryField() {
            Airport created = airportService.createAirport(
                    request("OPO", "Porto", 41.24, -8.68, "Europe/Lisbon"));

            // Lombok's @EqualsAndHashCode compares all fields, so one isEqualTo checks the whole mapping
            assertThat(created).isEqualTo(
                    new Airport("OPO", "Porto", "OPO", "PT", 41.24, -8.68, "Europe/Lisbon"));
        }

        @Test
        void shouldMakeCreatedAirportAvailableForLookup() {
            Airport created = airportService.createAirport(
                    request("OPO", "Porto", 41.24, -8.68, "Europe/Lisbon"));

            assertThat(airportService.getAirportByCode("OPO")).isSameAs(created);
            assertThat(airportService.getAllAirports()).containsExactly(LISBON, created);
        }

        @Test
        void shouldRejectDuplicateCode() {
            assertThatThrownBy(() -> airportService.createAirport(
                    request("LIS", "Another Lisbon", 0.0, 0.0, "Europe/Lisbon")))
                    .isInstanceOf(AirportAlreadyExistsException.class)
                    .hasMessageContaining("LIS");
        }

        @Test
        void shouldRejectDuplicateCodeIgnoringCase() {
            // The DTO only allows upper case, but the service must not rely on that:
            // if another caller passes "lis", it is still the same airport.
            assertThatThrownBy(() -> airportService.createAirport(
                    request("lis", "Another Lisbon", 0.0, 0.0, "Europe/Lisbon")))
                    .isInstanceOf(AirportAlreadyExistsException.class);
        }

        @Test
        void shouldNotStoreAirportWhenRejected() {
            try {
                airportService.createAirport(request("LIS", "Another Lisbon", 0.0, 0.0, "Europe/Lisbon"));
            } catch (AirportAlreadyExistsException expected) {
                // covered above; here we only check side effects
            }

            assertThat(airportService.getAllAirports()).containsExactly(LISBON);
            assertThat(airportService.getAirportByCode("LIS").getName()).isEqualTo("Lisbon");
        }
    }

    // ------------------------------------------------------------------ test data helpers

    /** A Portuguese airport whose city code equals its airport code; only the varying fields are parameters. */
    private static CreateAirportRequest request(String code, String name,
                                                double latitude, double longitude, String timeZoneId) {
        return new CreateAirportRequest(code, name, code, "PT", latitude, longitude, timeZoneId);
    }
}
