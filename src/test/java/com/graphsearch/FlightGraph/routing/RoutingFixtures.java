package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A small hand-made network where every expected answer can be worked out on paper.
 * All times are UTC ("Z") to keep the arithmetic easy; time-zone cases have their own tests.
 * Main travel day: 2027-05-10. Default rules: min connection 45 min, max layover 24 h.
 *
 * <pre>
 *                 ┌──────────── LIS-NRT-1000 (direct, €900, arr 22:00) ─────────────┐
 *                 │                                                                 ▼
 *   LIS ──0800/1500──► FRA ──1130/1200/1300/2000/2200/D2-0900/D2-1900──────────► NRT
 *    │                  ▲                                                         ▲
 *    └──0900──► MAD ─1015/1050─┘                                                  │
 *                │  └──1100──────────────────────────────────────────────────────┤
 *                └──1105──► ZRH ──1400 (arr 21:30)───────────────────────────────┘
 * </pre>
 *
 * Expected answers LIS → NRT on 2027-05-10:
 * - 1-stop finder: the 10 itineraries in {@link #EXPECTED_ONE_STOP_LIS_NRT}.
 * - Earliest arrival: max 0-1 stops → direct (22:00); max 2+ stops → via MAD+ZRH (21:30).
 * - Cheapest: 0 stops → €900; 1 stop → LIS-FRA-0800 + FRA-NRT-1300 = €300;
 *             2+ stops → LIS-MAD-0900 + MAD-FRA-1050 + FRA-NRT-1300 = €260.
 * Traps: FRA-NRT-1130 (30 min after LIS-FRA-0800: too short), MAD-FRA-1015 (15 min: too short,
 * but would make a €245 route), FRA-NRT-D2-1900 (layover > 24 h), FRA-NRT-2200 (no price),
 * FRA-LIS-1900 (goes back to the origin), LIS-ZRH-D2-0700 (departs the next day).
 */
public final class RoutingFixtures {

    private RoutingFixtures() {}

    public static final LocalDate DAY = LocalDate.parse("2027-05-10");

    public static final Flight LIS_FRA_0800 = flight("LIS-FRA-0800", "LIS", "FRA", "2027-05-10T08:00Z", "2027-05-10T11:00Z", 100.0);
    public static final Flight LIS_FRA_1500 = flight("LIS-FRA-1500", "LIS", "FRA", "2027-05-10T15:00Z", "2027-05-10T18:00Z", 80.0);
    public static final Flight LIS_MAD_0900 = flight("LIS-MAD-0900", "LIS", "MAD", "2027-05-10T09:00Z", "2027-05-10T10:00Z", 40.0);
    public static final Flight LIS_NRT_1000 = flight("LIS-NRT-1000", "LIS", "NRT", "2027-05-10T10:00Z", "2027-05-10T22:00Z", 900.0);
    public static final Flight LIS_ZRH_D2_0700 = flight("LIS-ZRH-D2-0700", "LIS", "ZRH", "2027-05-11T07:00Z", "2027-05-11T10:00Z", 120.0);

    public static final Flight FRA_NRT_1130 = flight("FRA-NRT-1130", "FRA", "NRT", "2027-05-10T11:30Z", "2027-05-10T23:30Z", 500.0);
    public static final Flight FRA_NRT_1200 = flight("FRA-NRT-1200", "FRA", "NRT", "2027-05-10T12:00Z", "2027-05-11T00:00Z", 450.0);
    public static final Flight FRA_NRT_1300 = flight("FRA-NRT-1300", "FRA", "NRT", "2027-05-10T13:00Z", "2027-05-11T01:00Z", 200.0);
    public static final Flight FRA_NRT_2000 = flight("FRA-NRT-2000", "FRA", "NRT", "2027-05-10T20:00Z", "2027-05-11T08:00Z", 300.0);
    public static final Flight FRA_NRT_2200 = flight("FRA-NRT-2200", "FRA", "NRT", "2027-05-10T22:00Z", "2027-05-11T10:00Z", null);
    public static final Flight FRA_NRT_D2_0900 = flight("FRA-NRT-D2-0900", "FRA", "NRT", "2027-05-11T09:00Z", "2027-05-11T21:00Z", 350.0);
    public static final Flight FRA_NRT_D2_1900 = flight("FRA-NRT-D2-1900", "FRA", "NRT", "2027-05-11T19:00Z", "2027-05-12T07:00Z", 100.0);
    public static final Flight FRA_LIS_1900 = flight("FRA-LIS-1900", "FRA", "LIS", "2027-05-10T19:00Z", "2027-05-10T22:00Z", 90.0);

    public static final Flight MAD_NRT_1100 = flight("MAD-NRT-1100", "MAD", "NRT", "2027-05-10T11:00Z", "2027-05-10T23:00Z", 700.0);
    public static final Flight MAD_FRA_1015 = flight("MAD-FRA-1015", "MAD", "FRA", "2027-05-10T10:15Z", "2027-05-10T11:15Z", 5.0);
    public static final Flight MAD_FRA_1050 = flight("MAD-FRA-1050", "MAD", "FRA", "2027-05-10T10:50Z", "2027-05-10T11:50Z", 20.0);
    public static final Flight MAD_ZRH_1105 = flight("MAD-ZRH-1105", "MAD", "ZRH", "2027-05-10T11:05Z", "2027-05-10T13:05Z", 60.0);

    public static final Flight ZRH_NRT_1400 = flight("ZRH-NRT-1400", "ZRH", "NRT", "2027-05-10T14:00Z", "2027-05-10T21:30Z", 650.0);

    public static List<Flight> all() {
        return List.of(
                LIS_FRA_0800, LIS_FRA_1500, LIS_MAD_0900, LIS_NRT_1000, LIS_ZRH_D2_0700,
                FRA_NRT_1130, FRA_NRT_1200, FRA_NRT_1300, FRA_NRT_2000, FRA_NRT_2200,
                FRA_NRT_D2_0900, FRA_NRT_D2_1900, FRA_LIS_1900,
                MAD_NRT_1100, MAD_FRA_1015, MAD_FRA_1050, MAD_ZRH_1105,
                ZRH_NRT_1400);
    }

    public static FlightNetwork network() {
        return new FlightNetwork(all());
    }

    /** Leg ids of every valid itinerary LIS → NRT on DAY with at most 1 stop. */
    public static final List<List<String>> EXPECTED_ONE_STOP_LIS_NRT = List.of(
            List.of("LIS-NRT-1000"),
            List.of("LIS-MAD-0900", "MAD-NRT-1100"),
            List.of("LIS-FRA-0800", "FRA-NRT-1200"),
            List.of("LIS-FRA-0800", "FRA-NRT-1300"),
            List.of("LIS-FRA-0800", "FRA-NRT-2000"),
            List.of("LIS-FRA-0800", "FRA-NRT-2200"),
            List.of("LIS-FRA-0800", "FRA-NRT-D2-0900"), // 22 h layover, next day: allowed
            List.of("LIS-FRA-1500", "FRA-NRT-2000"),
            List.of("LIS-FRA-1500", "FRA-NRT-2200"),
            List.of("LIS-FRA-1500", "FRA-NRT-D2-0900")); // crosses midnight: allowed

    public static Flight flight(String id, String origin, String destination,
                                String departure, String arrival, Double price) {
        return new Flight(id, origin, destination, OffsetDateTime.parse(departure), OffsetDateTime.parse(arrival),
                price, "TP", id);
    }

    public static List<List<String>> legIds(List<Itinerary> itineraries) {
        return itineraries.stream().map(RoutingFixtures::legIds).toList();
    }

    public static List<String> legIds(Itinerary itinerary) {
        return itinerary.legs().stream().map(Flight::getId).toList();
    }
}
