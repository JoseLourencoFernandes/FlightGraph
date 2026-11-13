package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.OffsetDateTime;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.flight;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ROADMAP 2.1 — when is a connection possible. */
class ConnectionRulesTest {

    private static final ConnectionRules RULES = ConnectionRules.DEFAULT; // 45 min .. 24 h
    private static final Flight ARRIVING = flight("IN", "LIS", "FRA", "2027-05-10T08:00Z", "2027-05-10T11:00Z", 100.0);

    @Test
    void defaultsShouldBe45MinutesTo24Hours() {
        assertThat(RULES.minConnectionTime()).isEqualTo(Duration.ofMinutes(45));
        assertThat(RULES.maxLayover()).isEqualTo(Duration.ofHours(24));
    }

    @ParameterizedTest(name = "layover of {0} min allowed = {1}")
    @CsvSource({
            "44,   false",  // just below the minimum
            "45,   true",   // exactly the minimum
            "120,  true",
            "1440, true",   // exactly 24 h
            "1441, false",  // just above the maximum
    })
    void shouldAllowLayoversWithinBoundsInclusive(long layoverMinutes, boolean allowed) {
        OffsetDateTime departure = ARRIVING.getArrivalTime().plusMinutes(layoverMinutes);
        Flight departing = flight("OUT", "FRA", "NRT", departure.toString(), departure.plusHours(12).toString(), 100.0);

        assertThat(RULES.allows(ARRIVING, departing)).isEqualTo(allowed);
    }

    @Test
    void shouldRejectFlightDepartingBeforeArrival() {
        Flight departing = flight("OUT", "FRA", "NRT", "2027-05-10T10:00Z", "2027-05-10T22:00Z", 100.0);

        assertThat(RULES.allows(ARRIVING, departing)).isFalse();
    }

    @Test
    void shouldRejectConnectionFromAnotherAirport() {
        Flight departing = flight("OUT", "MUC", "NRT", "2027-05-10T13:00Z", "2027-05-11T01:00Z", 100.0);

        assertThat(RULES.allows(ARRIVING, departing)).isFalse();
    }

    @Test
    void shouldMeasureLayoverBetweenInstantsNotClockTimes() {
        // Arrives 11:00Z; departs "12:30" Frankfurt summer time = 10:30Z -> before arrival.
        // Comparing local clock times (11:00 vs 12:30) would wrongly say 90 min.
        Flight departing = flight("OUT", "FRA", "NRT", "2027-05-10T12:30+02:00", "2027-05-11T07:00+09:00", 100.0);

        assertThat(RULES.allows(ARRIVING, departing)).isFalse();
    }

    @Test
    void shouldRejectNegativeMinimum() {
        assertThatThrownBy(() -> new ConnectionRules(Duration.ofMinutes(-1), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMaximumBelowMinimum() {
        assertThatThrownBy(() -> new ConnectionRules(Duration.ofHours(2), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNulls() {
        assertThatThrownBy(() -> new ConnectionRules(null, Duration.ofHours(1)))
                .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
    }
}
