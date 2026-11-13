package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ROADMAP 2.1 — the result type of every search. */
class ItineraryTest {

    @Nested
    class DirectFlight {

        private final Itinerary direct = new Itinerary(List.of(LIS_NRT_1000));

        @Test
        void shouldDescribeEndpointsAndTimes() {
            assertThat(direct.origin()).isEqualTo("LIS");
            assertThat(direct.destination()).isEqualTo("NRT");
            assertThat(direct.departureTime()).isEqualTo(OffsetDateTime.parse("2027-05-10T10:00Z"));
            assertThat(direct.arrivalTime()).isEqualTo(OffsetDateTime.parse("2027-05-10T22:00Z"));
        }

        @Test
        void shouldHaveNoStopsAndNoLayovers() {
            assertThat(direct.stops()).isZero();
            assertThat(direct.layovers()).isEmpty();
        }

        @Test
        void shouldUseFlightDurationAndPrice() {
            assertThat(direct.totalDuration()).isEqualTo(Duration.ofHours(12));
            assertThat(direct.totalPrice()).isEqualTo(900.0);
        }
    }

    @Nested
    class MultiLeg {

        private final Itinerary twoStops = new Itinerary(List.of(LIS_MAD_0900, MAD_FRA_1050, FRA_NRT_1300));

        @Test
        void shouldDescribeEndpointsFromFirstAndLastLeg() {
            assertThat(twoStops.origin()).isEqualTo("LIS");
            assertThat(twoStops.destination()).isEqualTo("NRT");
            assertThat(twoStops.departureTime()).isEqualTo(LIS_MAD_0900.getDepartureTime());
            assertThat(twoStops.arrivalTime()).isEqualTo(FRA_NRT_1300.getArrivalTime());
        }

        @Test
        void shouldCountStopsAndLayovers() {
            assertThat(twoStops.stops()).isEqualTo(2);
            // MAD: 10:00 -> 10:50, FRA: 11:50 -> 13:00
            assertThat(twoStops.layovers()).containsExactly(Duration.ofMinutes(50), Duration.ofMinutes(70));
        }

        @Test
        void shouldMeasureTotalDurationIncludingLayovers() {
            // 09:00 on the 10th -> 01:00 on the 11th
            assertThat(twoStops.totalDuration()).isEqualTo(Duration.ofHours(16));
        }

        @Test
        void shouldSumPrices() {
            assertThat(twoStops.totalPrice()).isEqualTo(260.0);
        }

        @Test
        void shouldMeasureDurationAcrossTimeZones() {
            // Departs 08:00 Lisbon (07:00Z), arrives 13:00 Frankfurt (11:00Z): 4 h, not 5 h
            Flight leg = flight("TZ", "LIS", "FRA", "2027-05-10T08:00+01:00", "2027-05-10T13:00+02:00", 100.0);

            assertThat(new Itinerary(List.of(leg)).totalDuration()).isEqualTo(Duration.ofHours(4));
        }
    }

    @Nested
    class UnknownPrice {

        @Test
        void shouldHaveNullTotalWhenAnyLegHasNoPrice() {
            Itinerary itinerary = new Itinerary(List.of(LIS_FRA_1500, FRA_NRT_2200)); // FRA_NRT_2200 has no price

            assertThat(itinerary.totalPrice()).isNull(); // NOT 80.0 — unknown is not free
        }
    }

    @Nested
    class Construction {

        @Test
        void shouldRejectEmptyLegs() {
            assertThatThrownBy(() -> new Itinerary(List.of())).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectNullLegs() {
            assertThatThrownBy(() -> new Itinerary(null))
                    .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
        }

        @Test
        void shouldRejectLegsThatAreNotChained() {
            // arrives in FRA, next leg leaves from MAD
            assertThatThrownBy(() -> new Itinerary(List.of(LIS_FRA_0800, MAD_NRT_1100)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectLegDepartingBeforePreviousArrives() {
            // LIS_FRA_1500 arrives 18:00, FRA_NRT_1200 departs 12:00
            assertThatThrownBy(() -> new Itinerary(List.of(LIS_FRA_1500, FRA_NRT_1200)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldAllowShortConnectionsBecauseRulesAreCheckedElsewhere() {
            // 30 min is below the 45 min rule, but structurally valid: ConnectionRules is the search's job
            Itinerary itinerary = new Itinerary(List.of(LIS_FRA_0800, FRA_NRT_1130));

            assertThat(itinerary.layovers()).containsExactly(Duration.ofMinutes(30));
        }

        @Test
        void shouldBeImmutable() {
            List<Flight> legs = new ArrayList<>(List.of(LIS_FRA_0800, FRA_NRT_1200));
            Itinerary itinerary = new Itinerary(legs);

            legs.clear();

            assertThat(itinerary.legs()).containsExactly(LIS_FRA_0800, FRA_NRT_1200);
            assertThatThrownBy(() -> itinerary.legs().add(FRA_LIS_1900))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
