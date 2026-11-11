package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

/** ROADMAP 2.3 — earliest arrival with up to N stops. */
@Disabled("ROADMAP 2.3 — delete this line when you start the earliest-arrival search")
class EarliestArrivalSearchTest {

    private final ConnectionRules rules = ConnectionRules.DEFAULT;
    private final EarliestArrivalSearch search = new EarliestArrivalSearch(network(), rules);

    @ParameterizedTest(name = "maxStops={0} -> {1}")
    @CsvSource({
            "0, LIS-NRT-1000",                           // direct, 22:00
            "1, LIS-NRT-1000",                           // nothing with 1 stop beats 22:00
            "2, LIS-MAD-0900|MAD-ZRH-1105|ZRH-NRT-1400", // 21:30 needs 2 stops
            "3, LIS-MAD-0900|MAD-ZRH-1105|ZRH-NRT-1400",
    })
    void shouldFindEarliestArrivalForEachStopLimit(int maxStops, String expectedLegs) {
        Optional<Itinerary> result = search.find(new RouteQuery("LIS", "NRT", DAY, maxStops));

        assertThat(result).isPresent();
        assertThat(legIds(result.get())).containsExactly(expectedLegs.split("\\|"));
    }

    @Test
    void shouldReturnAValidItinerary() {
        RouteQuery query = new RouteQuery("LIS", "NRT", DAY, 3);

        ReferenceRoutes.assertValid(search.find(query).orElseThrow(), query, rules);
    }

    @Test
    void shouldIgnoreConnectionsThatAreTooShort() {
        // LIS-MAD-0900 + MAD-FRA-1015 (15 min) would reach FRA at 11:15 and catch FRA-NRT-1200.
        // Only valid connections may be used, so the 2-stop answer must not contain it.
        Itinerary result = search.find(new RouteQuery("LIS", "NRT", DAY, 2)).orElseThrow();

        assertThat(legIds(result)).doesNotContain("MAD-FRA-1015");
    }

    @Test
    void shouldReturnEmptyWhenUnreachable() {
        assertThat(search.find(new RouteQuery("NRT", "LIS", DAY, 3))).isEmpty();
        assertThat(search.find(new RouteQuery("LIS", "NRT", DAY.plusDays(5), 3))).isEmpty();
    }

    @Test
    void shouldNotRouteBackThroughTheOrigin() {
        // The only way to NRT would be LIS -> FRA -> LIS -> NRT (revisits LIS). The last leg departs on the
        // next day, so it isn't a valid direct option either: there is no valid itinerary at all.
        List<Flight> flights = List.of(
                flight("OUT", "LIS", "FRA", "2027-05-10T20:00Z", "2027-05-10T22:00Z", 10.0),
                flight("BACK", "FRA", "LIS", "2027-05-11T06:00Z", "2027-05-11T08:00Z", 10.0),
                flight("NEXT-DAY", "LIS", "NRT", "2027-05-11T09:00Z", "2027-05-11T21:00Z", 100.0));
        EarliestArrivalSearch cyclic = new EarliestArrivalSearch(new FlightNetwork(flights), rules);

        assertThat(cyclic.find(new RouteQuery("LIS", "NRT", DAY, 3))).isEmpty();
    }

    @Test
    void shouldRespectTheStopLimitEvenWhenMoreStopsArriveEarlier() {
        // A -> B -> C -> D arrives at 12:00 but needs 2 stops; A -> D direct arrives at 20:00.
        // The earliest arrival at C is via B (1 stop) — the search must still not exceed maxStops = 1.
        List<Flight> flights = List.of(
                flight("A-B", "AAA", "BBB", "2027-05-10T06:00Z", "2027-05-10T07:00Z", 10.0),
                flight("B-C", "BBB", "CCC", "2027-05-10T08:00Z", "2027-05-10T09:00Z", 10.0),
                flight("C-D", "CCC", "DDD", "2027-05-10T10:00Z", "2027-05-10T12:00Z", 10.0),
                flight("A-C", "AAA", "CCC", "2027-05-10T06:00Z", "2027-05-10T13:00Z", 10.0),
                flight("C-D-late", "CCC", "DDD", "2027-05-10T14:00Z", "2027-05-10T15:00Z", 10.0),
                flight("A-D", "AAA", "DDD", "2027-05-10T07:00Z", "2027-05-10T20:00Z", 10.0));
        EarliestArrivalSearch limited = new EarliestArrivalSearch(new FlightNetwork(flights), rules);

        // maxStops=1: A-C (13:00) + C-D-late (15:00) beats the direct A-D (20:00)
        assertThat(legIds(limited.find(new RouteQuery("AAA", "DDD", DAY, 1)).orElseThrow()))
                .containsExactly("A-C", "C-D-late");
        // maxStops=2: A-B-C-D at 12:00
        assertThat(legIds(limited.find(new RouteQuery("AAA", "DDD", DAY, 2)).orElseThrow()))
                .containsExactly("A-B", "B-C", "C-D");
    }

    /** Differential test against brute force: same earliest arrival instant, and a valid itinerary. */
    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40})
    void shouldMatchBruteForceOnRandomNetworks(long seed) {
        List<Flight> flights = RandomNetworks.generate(seed, 40);
        EarliestArrivalSearch randomSearch = new EarliestArrivalSearch(new FlightNetwork(flights), rules);

        for (int maxStops = 0; maxStops <= 3; maxStops++) {
            RouteQuery query = new RouteQuery("AAA", "FFF", DAY, maxStops);
            Optional<Instant> expected = ReferenceRoutes.earliestArrival(flights, query, rules);
            Optional<Itinerary> actual = randomSearch.find(query);

            assertThat(actual.map(i -> i.arrivalTime().toInstant())).as("maxStops=%d", maxStops).isEqualTo(expected);
            actual.ifPresent(itinerary -> ReferenceRoutes.assertValid(itinerary, query, rules));
        }
    }
}
