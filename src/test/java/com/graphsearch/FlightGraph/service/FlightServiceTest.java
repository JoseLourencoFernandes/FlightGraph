package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.dto.CreateFlightRequest;
import com.graphsearch.FlightGraph.exception.FlightAlreadyExistsException;
import com.graphsearch.FlightGraph.exception.FlightNotFoundException;
import com.graphsearch.FlightGraph.exception.UnknownAirportException;
import com.graphsearch.FlightGraph.model.Airport;
import com.graphsearch.FlightGraph.model.Flight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests: no Spring context. FlightService is built with `new`, which constructor
 * injection makes possible. AirportService is used for real (not mocked) because it is a simple
 * in-memory collaborator — a mock would add setup without making the test more trustworthy.
 *
 * JUnit creates a new instance of this class for every @Test, so @BeforeEach gives each test
 * fresh services with no flights left over from other tests.
 */
class FlightServiceTest {

    private FlightService flightService;

    @BeforeEach
    void setUp() {
        AirportService airportService = new AirportService();
        airportService.addAll(List.of(
                new Airport("LIS", "Lisbon", "LIS", "PT", 38.78, -9.13, "Europe/Lisbon"),
                new Airport("OPO", "Porto", "OPO", "PT", 41.24, -8.68, "Europe/Lisbon"),
                new Airport("FRA", "Frankfurt", "FRA", "DE", 50.03, 8.56, "Europe/Berlin")));
        flightService = new FlightService(airportService);
    }

    // @Nested groups tests by the method under test; test reports show them as a tree.
    @Nested
    class Search {

        @BeforeEach
        void seedFlights() {
            flightService.addAll(List.of(
                    flight("TP123-2027-05-10", "LIS", "OPO", "2027-05-10T08:00+01:00"),
                    flight("TP456-2027-05-10", "OPO", "LIS", "2027-05-10T12:00+01:00"),
                    flight("LH789-2027-05-11", "LIS", "FRA", "2027-05-11T09:00+01:00")));
        }

        @Test
        void shouldReturnAllFlightsWhenNoFilterGiven() {
            List<Flight> result = flightService.search(null, null, null);

            assertThat(result).hasSize(3);
        }

        @Test
        void shouldFilterFlightsByOrigin() {
            List<Flight> result = flightService.search("LIS", null, null);

            assertThat(result)
                    .extracting(Flight::getId)
                    .containsExactlyInAnyOrder("TP123-2027-05-10", "LH789-2027-05-11");
        }

        @Test
        void shouldSearchOriginIgnoringCase() {
            List<Flight> result = flightService.search("lis", null, null);

            assertThat(result).hasSize(2);
        }

        @Test
        void shouldFilterFlightsByDestination() {
            List<Flight> result = flightService.search(null, "FRA", null);

            assertThat(result).extracting(Flight::getId).containsExactly("LH789-2027-05-11");
        }

        @Test
        void shouldCombineAllFilters() {
            List<Flight> result = flightService.search("LIS", "OPO", LocalDate.parse("2027-05-10"));

            assertThat(result).extracting(Flight::getId).containsExactly("TP123-2027-05-10");
        }

        @Test
        void shouldMatchDateAgainstLocalDepartureDateNotUtc() {
            // 00:30 in Lisbon on the 12th is still 23:30 UTC on the 11th.
            // A person searching "flights on the 12th" expects to find it.
            flightService.addAll(List.of(flight("TP999-2027-05-12", "LIS", "FRA", "2027-05-12T00:30+01:00")));

            assertThat(flightService.search(null, null, LocalDate.parse("2027-05-12")))
                    .extracting(Flight::getId).containsExactly("TP999-2027-05-12");
            assertThat(flightService.search(null, null, LocalDate.parse("2027-05-11")))
                    .extracting(Flight::getId).doesNotContain("TP999-2027-05-12");
        }

        @Test
        void shouldReturnEmptyListWhenNothingMatches() {
            assertThat(flightService.search("FRA", "OPO", null)).isEmpty();
        }
    }

    @Nested
    class GetFlightById {

        @Test
        void shouldReturnFlightWhenIdExists() {
            Flight flight = flight("TP123-2027-05-10", "LIS", "OPO", "2027-05-10T08:00+01:00");
            flightService.addAll(List.of(flight));

            Flight result = flightService.getFlightById("TP123-2027-05-10");

            assertThat(result).isSameAs(flight);
        }

        @Test
        void shouldThrowWhenIdDoesNotExist() {
            assertThatThrownBy(() -> flightService.getFlightById("NOPE"))
                    .isInstanceOf(FlightNotFoundException.class)
                    .hasMessageContaining("NOPE");
        }
    }

    @Nested
    class CreateFlight {

        @Test
        void shouldCreateFlightAndMakeItSearchable() {
            Flight created = flightService.createFlight(
                    request("LIS", "FRA", "2027-05-10T10:00Z", "2027-05-10T13:00Z"));

            assertThat(created.getId()).isEqualTo("TP999-2027-05-10");
            assertThat(created.getOrigin()).isEqualTo("LIS");
            assertThat(created.getPrice()).isEqualTo(99.5);
            assertThat(flightService.getFlightById("TP999-2027-05-10")).isSameAs(created);
        }

        @Test
        void shouldNormalizeTimesToEachAirportTimeZone() {
            // The client sends UTC; we store the times in origin/destination local offsets.
            Flight created = flightService.createFlight(
                    request("LIS", "FRA", "2027-05-10T10:00Z", "2027-05-10T13:00Z"));

            assertThat(created.getDepartureTime().getOffset()).isEqualTo(ZoneOffset.ofHours(1)); // Lisbon, summer
            assertThat(created.getArrivalTime().getOffset()).isEqualTo(ZoneOffset.ofHours(2));   // Frankfurt, summer
            // Same instant, only the representation changed
            assertThat(created.getDepartureTime().toInstant())
                    .isEqualTo(OffsetDateTime.parse("2027-05-10T10:00Z").toInstant());
        }

        @Test
        void shouldUseOriginLocalDateInId() {
            // 23:30 UTC on the 10th is already 00:30 on the 11th in Lisbon
            Flight created = flightService.createFlight(
                    request("LIS", "FRA", "2027-05-10T23:30Z", "2027-05-11T02:30Z"));

            assertThat(created.getId()).isEqualTo("TP999-2027-05-11");
        }

        @Test
        void shouldRejectUnknownOrigin() {
            assertThatThrownBy(() -> flightService.createFlight(
                    request("XYZ", "FRA", "2027-05-10T10:00Z", "2027-05-10T13:00Z")))
                    .isInstanceOf(UnknownAirportException.class)
                    .hasMessageContaining("origin")
                    .hasMessageContaining("XYZ");
        }

        @Test
        void shouldRejectUnknownDestination() {
            assertThatThrownBy(() -> flightService.createFlight(
                    request("LIS", "XYZ", "2027-05-10T10:00Z", "2027-05-10T13:00Z")))
                    .isInstanceOf(UnknownAirportException.class)
                    .hasMessageContaining("destination");
        }

        @Test
        void shouldRejectDuplicateFlightOnSameLocalDay() {
            flightService.createFlight(request("LIS", "FRA", "2027-05-10T10:00Z", "2027-05-10T13:00Z"));

            assertThatThrownBy(() -> flightService.createFlight(
                    request("LIS", "FRA", "2027-05-10T15:00Z", "2027-05-10T18:00Z")))
                    .isInstanceOf(FlightAlreadyExistsException.class)
                    .hasMessageContaining("TP999-2027-05-10");
        }

        @Test
        void shouldNotStoreFlightWhenRejected() {
            try {
                flightService.createFlight(request("LIS", "XYZ", "2027-05-10T10:00Z", "2027-05-10T13:00Z"));
            } catch (UnknownAirportException expected) {
                // the exception itself is covered above; here we only care about side effects
            }

            assertThat(flightService.getAllFlights()).isEmpty();
        }
    }

    // ------------------------------------------------------------------ test data helpers

    /** A flight with only the fields the search cares about; duration fixed at 2h. */
    private static Flight flight(String id, String origin, String destination, String departure) {
        OffsetDateTime dep = OffsetDateTime.parse(departure);
        return new Flight(id, origin, destination, dep, dep.plusHours(2), 100.0, "TP", "123");
    }

    /** A valid request for flight TP999; only route and times vary between tests. */
    private static CreateFlightRequest request(String origin, String destination,
                                               String departure, String arrival) {
        return new CreateFlightRequest(origin, destination,
                OffsetDateTime.parse(departure), OffsetDateTime.parse(arrival),
                99.5, "TP", "999");
    }
}
