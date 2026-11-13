package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ROADMAP 2.1 — the graph structure. */
class FlightNetworkTest {

    @Test
    void shouldListDeparturesOfAnAirportOrderedByDeparture() {
        FlightNetwork network = network();

        assertThat(network.departuresFrom("MAD")).containsExactly(MAD_FRA_1015, MAD_FRA_1050, MAD_NRT_1100, MAD_ZRH_1105);
    }

    @Test
    void shouldOrderByInstantEvenWhenInputIsUnsorted() {
        FlightNetwork network = new FlightNetwork(List.of(FRA_NRT_2000, FRA_NRT_1130, FRA_NRT_D2_0900, FRA_NRT_1200));

        assertThat(network.departuresFrom("FRA")).containsExactly(FRA_NRT_1130, FRA_NRT_1200, FRA_NRT_2000, FRA_NRT_D2_0900);
    }

    @Test
    void shouldOrderByRealInstantNotLocalClockTime() {
        // "08:30" in Lisbon summer time is 07:30 UTC — earlier than "08:00 UTC", despite the bigger clock time
        Flight lisbonLocal = flight("A", "LIS", "FRA", "2027-05-10T08:30+01:00", "2027-05-10T12:30+02:00", 100.0);
        Flight utc = flight("B", "LIS", "MAD", "2027-05-10T08:00Z", "2027-05-10T09:00Z", 100.0);

        FlightNetwork network = new FlightNetwork(List.of(utc, lisbonLocal));

        assertThat(network.departuresFrom("LIS")).containsExactly(lisbonLocal, utc);
    }

    @Test
    void shouldReturnEmptyListForUnknownAirport() {
        assertThat(network().departuresFrom("XYZ")).isEmpty();
    }

    @Test
    void shouldNotListArrivalsAsDepartures() {
        // NRT only receives flights in the fixtures
        assertThat(network().departuresFrom("NRT")).isEmpty();
    }

    @Test
    void shouldReturnDeparturesWithinWindowInclusive() {
        FlightNetwork network = network();

        List<Flight> result = network.departuresBetween("FRA",
                Instant.parse("2027-05-10T12:00:00Z"), Instant.parse("2027-05-10T20:00:00Z"));

        // 12:00 and 20:00 are exactly on the bounds and must be included
        assertThat(result).containsExactly(FRA_NRT_1200, FRA_NRT_1300, FRA_LIS_1900, FRA_NRT_2000);
    }

    @Test
    void shouldReturnEmptyWindowWhenNothingDeparts() {
        assertThat(network().departuresBetween("FRA",
                Instant.parse("2027-05-10T00:00:00Z"), Instant.parse("2027-05-10T11:00:00Z"))).isEmpty();
    }

    @Test
    void shouldCompareWindowBoundsAsInstants() {
        Flight lisbonLocal = flight("A", "LIS", "FRA", "2027-05-10T08:30+01:00", "2027-05-10T12:30+02:00", 100.0);

        // 08:30+01:00 == 07:30Z
        assertThat(new FlightNetwork(List.of(lisbonLocal)).departuresBetween("LIS",
                Instant.parse("2027-05-10T07:30:00Z"), Instant.parse("2027-05-10T07:30:00Z"))).containsExactly(lisbonLocal);
    }

    @Test
    void shouldKnowEveryAirportIncludingArrivalOnlyOnes() {
        assertThat(network().airports()).containsExactlyInAnyOrder("LIS", "FRA", "MAD", "ZRH", "NRT");
    }

    @Test
    void shouldNotBeAffectedByLaterChangesToTheInputList() {
        List<Flight> input = new ArrayList<>(List.of(LIS_FRA_0800));
        FlightNetwork network = new FlightNetwork(input);

        input.add(LIS_FRA_1500);

        assertThat(network.departuresFrom("LIS")).containsExactly(LIS_FRA_0800);
    }

    @Test
    void shouldReturnUnmodifiableLists() {
        List<Flight> departures = network().departuresFrom("LIS");

        assertThatThrownBy(() -> departures.add(FRA_LIS_1900)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldHandleAnEmptyNetwork() {
        FlightNetwork empty = new FlightNetwork(List.of());

        assertThat(empty.airports()).isEmpty();
        assertThat(empty.departuresFrom("LIS")).isEmpty();
    }
}
