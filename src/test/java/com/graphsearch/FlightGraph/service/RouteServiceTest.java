package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.routing.Itinerary;
import com.graphsearch.FlightGraph.routing.RouteSort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests (no Spring) for RouteService, using the hand-made network from RoutingFixtures.
 * The algorithms are tested in routing/; here we only check that the service wires them up,
 * honours maxStops and orders the results.
 */
class RouteServiceTest {

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        FlightService flightService = new FlightService(new AirportService());
        flightService.addAll(all());
        routeService = new RouteService(flightService);
    }

    @Nested
    @Disabled("ROADMAP 2.2 — delete this line when you wire up RouteService")
    class UpToOneStop {

        @Test
        void shouldReturnEveryItinerary() {
            List<Itinerary> result = routeService.findRoutes("LIS", "NRT", DAY, 1, RouteSort.ARRIVAL);

            assertThat(legIds(result)).containsExactlyInAnyOrderElementsOf(EXPECTED_ONE_STOP_LIS_NRT);
        }

        @Test
        void shouldSortByArrival() {
            List<Itinerary> result = routeService.findRoutes("LIS", "NRT", DAY, 1, RouteSort.ARRIVAL);

            assertThat(result).isSortedAccordingTo(Comparator.comparing(i -> i.arrivalTime().toInstant()));
            assertThat(legIds(result.get(0))).containsExactly("LIS-NRT-1000");
        }

        @Test
        void shouldSortByPriceWithUnknownPricesLast() {
            List<Itinerary> result = routeService.findRoutes("LIS", "NRT", DAY, 1, RouteSort.PRICE);

            assertThat(legIds(result.get(0))).containsExactly("LIS-FRA-0800", "FRA-NRT-1300"); // €300
            assertThat(result).isSortedAccordingTo(
                    Comparator.comparing(Itinerary::totalPrice, Comparator.nullsLast(Comparator.naturalOrder())));
            assertThat(result.get(result.size() - 1).totalPrice()).isNull();
        }

        @Test
        void shouldReturnOnlyDirectFlightsWhenMaxStopsIsZero() {
            assertThat(legIds(routeService.findRoutes("LIS", "NRT", DAY, 0, RouteSort.ARRIVAL)))
                    .containsExactly(List.of("LIS-NRT-1000"));
        }

        @Test
        void shouldReturnEmptyListWhenNothingMatches() {
            assertThat(routeService.findRoutes("NRT", "LIS", DAY, 1, RouteSort.ARRIVAL)).isEmpty();
        }

        @Test
        void shouldSeeFlightsAddedAfterStartup() {
            // POST /flights adds flights at runtime; routes must take them into account
            FlightService flightService = new FlightService(new AirportService());
            RouteService service = new RouteService(flightService);
            assertThat(service.findRoutes("LIS", "NRT", DAY, 1, RouteSort.ARRIVAL)).isEmpty();

            flightService.addAll(List.of(LIS_NRT_1000));

            assertThat(service.findRoutes("LIS", "NRT", DAY, 1, RouteSort.ARRIVAL)).hasSize(1);
        }
    }

    @Nested
    @Disabled("ROADMAP 2.3 — delete this line when RouteService uses the 2.3 searches")
    class MultipleStops {

        @Test
        void shouldPutTheEarliestArrivalFirst() {
            List<Itinerary> result = routeService.findRoutes("LIS", "NRT", DAY, 2, RouteSort.ARRIVAL);

            assertThat(legIds(result.get(0))).containsExactly("LIS-MAD-0900", "MAD-ZRH-1105", "ZRH-NRT-1400");
        }

        @Test
        void shouldPutTheCheapestFirst() {
            List<Itinerary> result = routeService.findRoutes("LIS", "NRT", DAY, 3, RouteSort.PRICE);

            assertThat(result.get(0).totalPrice()).isEqualTo(260.0);
        }

        @Test
        void shouldReturnEmptyListWhenUnreachable() {
            assertThat(routeService.findRoutes("NRT", "LIS", DAY, 3, RouteSort.PRICE)).isEmpty();
        }
    }
}
