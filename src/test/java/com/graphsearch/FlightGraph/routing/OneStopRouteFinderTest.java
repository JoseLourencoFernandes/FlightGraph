package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ROADMAP 2.2 — enumerate direct and one-stop itineraries. */
@Disabled("ROADMAP 2.2 — delete this line when you start the one-stop finder")
class OneStopRouteFinderTest {

    private final ConnectionRules rules = ConnectionRules.DEFAULT;
    private final OneStopRouteFinder finder = new OneStopRouteFinder(network(), rules);

    @Test
    void shouldFindEveryValidDirectAndOneStopItinerary() {
        List<Itinerary> result = finder.findAll(new RouteQuery("LIS", "NRT", DAY, 1));

        assertThat(legIds(result)).containsExactlyInAnyOrderElementsOf(EXPECTED_ONE_STOP_LIS_NRT);
    }

    @Test
    void shouldReturnOnlyValidItineraries() {
        RouteQuery query = new RouteQuery("LIS", "NRT", DAY, 1);

        finder.findAll(query).forEach(itinerary -> ReferenceRoutes.assertValid(itinerary, query, rules));
    }

    @Test
    void shouldRejectConnectionsShorterThanMinimum() {
        // LIS-FRA-0800 lands 11:00, FRA-NRT-1130 leaves 30 min later
        assertThat(legIds(finder.findAll(new RouteQuery("LIS", "NRT", DAY, 1))))
                .doesNotContain(List.of("LIS-FRA-0800", "FRA-NRT-1130"));
    }

    @Test
    void shouldRejectLayoversLongerThanMaximum() {
        assertThat(legIds(finder.findAll(new RouteQuery("LIS", "NRT", DAY, 1))))
                .doesNotContain(List.of("LIS-FRA-0800", "FRA-NRT-D2-1900"),  // 32 h
                                List.of("LIS-FRA-1500", "FRA-NRT-D2-1900")); // 25 h
    }

    @Test
    void shouldReturnOnlyDirectFlightsWhenMaxStopsIsZero() {
        assertThat(legIds(finder.findAll(new RouteQuery("LIS", "NRT", DAY, 0))))
                .containsExactly(List.of("LIS-NRT-1000"));
    }

    @Test
    void shouldOnlyStartOnTheRequestedDate() {
        // LIS-ZRH-D2-0700 leaves on the 11th; on the 10th the only way is via MAD
        assertThat(legIds(finder.findAll(new RouteQuery("LIS", "ZRH", DAY, 1))))
                .containsExactly(List.of("LIS-MAD-0900", "MAD-ZRH-1105"));
        assertThat(legIds(finder.findAll(new RouteQuery("LIS", "ZRH", DAY.plusDays(1), 1))))
                .containsExactly(List.of("LIS-ZRH-D2-0700"));
    }

    @Test
    void shouldUseTheLocalDepartureDateAtTheOrigin() {
        // 00:30 in Lisbon on the 11th is 23:30 UTC on the 10th: it belongs to the 11th
        Flight lateNight = flight("NIGHT", "LIS", "FRA", "2027-05-11T00:30+01:00", "2027-05-11T04:30+02:00", 100.0);
        OneStopRouteFinder nightFinder = new OneStopRouteFinder(new FlightNetwork(List.of(lateNight)), rules);

        assertThat(nightFinder.findAll(new RouteQuery("LIS", "FRA", LocalDate.parse("2027-05-11"), 0))).hasSize(1);
        assertThat(nightFinder.findAll(new RouteQuery("LIS", "FRA", DAY, 0))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenUnreachable() {
        assertThat(finder.findAll(new RouteQuery("NRT", "LIS", DAY, 1))).isEmpty();
        assertThat(finder.findAll(new RouteQuery("XYZ", "LIS", DAY, 1))).isEmpty();
    }

    @Test
    void shouldHonourCustomRules() {
        // With a 30 min minimum, the 30 min connection becomes valid
        OneStopRouteFinder relaxed = new OneStopRouteFinder(network(),
                new ConnectionRules(Duration.ofMinutes(30), Duration.ofHours(24)));

        assertThat(legIds(relaxed.findAll(new RouteQuery("LIS", "NRT", DAY, 1))))
                .contains(List.of("LIS-FRA-0800", "FRA-NRT-1130"));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void shouldRejectMoreThanOneStop(int maxStops) {
        assertThatThrownBy(() -> finder.findAll(new RouteQuery("LIS", "NRT", DAY, maxStops)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Differential test: on random networks, must match the brute-force reference exactly. */
    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20})
    void shouldMatchBruteForceOnRandomNetworks(long seed) {
        List<Flight> flights = RandomNetworks.generate(seed, 40);
        OneStopRouteFinder randomFinder = new OneStopRouteFinder(new FlightNetwork(flights), rules);
        RouteQuery query = new RouteQuery("AAA", "FFF", DAY, 1);

        List<List<String>> expected = ReferenceRoutes.allPaths(flights, query, rules).stream()
                .map(path -> path.stream().map(Flight::getId).toList()).toList();

        assertThat(legIds(randomFinder.findAll(query))).containsExactlyInAnyOrderElementsOf(expected);
    }
}
