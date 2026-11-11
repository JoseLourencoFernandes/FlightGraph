package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

/** ROADMAP 2.3 — lowest total price with up to N stops. */
@Disabled("ROADMAP 2.3 — delete this line when you start the cheapest search")
class CheapestSearchTest {

    private final ConnectionRules rules = ConnectionRules.DEFAULT;
    private final CheapestSearch search = new CheapestSearch(network(), rules);

    @ParameterizedTest(name = "maxStops={0} -> €{1}")
    @CsvSource({
            "0, 900.0, LIS-NRT-1000",
            "1, 300.0, LIS-FRA-0800|FRA-NRT-1300",
            "2, 260.0, LIS-MAD-0900|MAD-FRA-1050|FRA-NRT-1300",
            "3, 260.0, LIS-MAD-0900|MAD-FRA-1050|FRA-NRT-1300",
    })
    void shouldFindCheapestForEachStopLimit(int maxStops, double expectedPrice, String expectedLegs) {
        Itinerary result = search.find(new RouteQuery("LIS", "NRT", DAY, maxStops)).orElseThrow();

        assertThat(result.totalPrice()).isEqualTo(expectedPrice);
        assertThat(legIds(result)).containsExactly(expectedLegs.split("\\|"));
    }

    @Test
    void shouldNotKeepOnlyTheCheapestWayToAnIntermediateAirport() {
        // The cheapest way into FRA is LIS-FRA-1500 (€80), but it lands at 18:00 and misses
        // FRA-NRT-1300 (€200). A search that only remembers "cheapest so far at FRA" ends up
        // with 80 + 300 = €380 instead of the real 1-stop answer, 100 + 200 = €300.
        Itinerary result = search.find(new RouteQuery("LIS", "NRT", DAY, 1)).orElseThrow();

        assertThat(result.totalPrice()).isEqualTo(300.0);
    }

    @Test
    void shouldIgnoreConnectionsThatAreTooShort() {
        // LIS-MAD-0900 + MAD-FRA-1015 (15 min) + FRA-NRT-1300 would cost €245
        Itinerary result = search.find(new RouteQuery("LIS", "NRT", DAY, 2)).orElseThrow();

        assertThat(result.totalPrice()).isEqualTo(260.0);
        assertThat(legIds(result)).doesNotContain("MAD-FRA-1015");
    }

    @Test
    void shouldNeverUseFlightsWithUnknownPrice() {
        // Via FRA would be "€10 + unknown"; the only fully priced option is the direct flight
        List<Flight> flights = List.of(
                flight("LIS-FRA", "LIS", "FRA", "2027-05-10T08:00Z", "2027-05-10T11:00Z", 10.0),
                flight("FRA-NRT-unpriced", "FRA", "NRT", "2027-05-10T13:00Z", "2027-05-11T01:00Z", null),
                flight("LIS-NRT", "LIS", "NRT", "2027-05-10T10:00Z", "2027-05-10T22:00Z", 900.0));
        CheapestSearch priced = new CheapestSearch(new FlightNetwork(flights), rules);

        assertThat(legIds(priced.find(new RouteQuery("LIS", "NRT", DAY, 1)).orElseThrow())).containsExactly("LIS-NRT");
    }

    @Test
    void shouldReturnEmptyWhenOnlyUnpricedRoutesExist() {
        List<Flight> flights = List.of(
                flight("LIS-NRT-unpriced", "LIS", "NRT", "2027-05-10T10:00Z", "2027-05-10T22:00Z", null));

        assertThat(new CheapestSearch(new FlightNetwork(flights), rules)
                .find(new RouteQuery("LIS", "NRT", DAY, 1))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenUnreachable() {
        assertThat(search.find(new RouteQuery("NRT", "LIS", DAY, 3))).isEmpty();
    }

    @Test
    void shouldNotRouteBackThroughTheOrigin() {
        List<Flight> flights = List.of(
                flight("OUT", "LIS", "FRA", "2027-05-10T20:00Z", "2027-05-10T22:00Z", 10.0),
                flight("BACK", "FRA", "LIS", "2027-05-11T06:00Z", "2027-05-11T08:00Z", 10.0),
                flight("NEXT-DAY", "LIS", "NRT", "2027-05-11T09:00Z", "2027-05-11T21:00Z", 100.0));

        assertThat(new CheapestSearch(new FlightNetwork(flights), rules)
                .find(new RouteQuery("LIS", "NRT", DAY, 3))).isEmpty();
    }

    /** Differential test against brute force: same lowest price, and a valid itinerary. */
    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40})
    void shouldMatchBruteForceOnRandomNetworks(long seed) {
        List<Flight> flights = RandomNetworks.generate(seed, 40);
        CheapestSearch randomSearch = new CheapestSearch(new FlightNetwork(flights), rules);

        for (int maxStops = 0; maxStops <= 3; maxStops++) {
            RouteQuery query = new RouteQuery("AAA", "FFF", DAY, maxStops);
            Optional<Double> expected = ReferenceRoutes.lowestPrice(flights, query, rules);
            Optional<Itinerary> actual = randomSearch.find(query);

            assertThat(actual.map(Itinerary::totalPrice)).as("maxStops=%d", maxStops).isEqualTo(expected);
            actual.ifPresent(itinerary -> ReferenceRoutes.assertValid(itinerary, query, rules));
        }
    }
}
