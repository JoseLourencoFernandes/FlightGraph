package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The flight network as a directed, temporal graph.
 *
 * Nodes are airports (IATA codes); every edge is ONE concrete flight with its own departure and
 * arrival instant, so there can be many edges between the same two airports.
 *
 * Plain Java on purpose (no Spring): easy to unit-test and to reason about.
 * Named FlightNetwork rather than FlightGraph to avoid clashing with the project/package name.
 *
 * Contract (see FlightNetworkTest):
 * - Immutable once built: later changes to the input collection don't affect it, and returned
 *   lists are unmodifiable.
 * - Departures are ordered by REAL instant, never by local wall-clock time (offsets can differ).
 */
public class FlightNetwork {

    private final Map<String, List<Flight>> departuresByAirport;
    private final Set<String> airports;

    public FlightNetwork(Collection<Flight> flights) {
        Map<String, List<Flight>> byOrigin = new HashMap<>();
        Set<String> allAirports = new HashSet<>();
        for (Flight flight : flights) {
            byOrigin.computeIfAbsent(flight.getOrigin(), k -> new ArrayList<>()).add(flight);
            // Destination-only airports (e.g. a hub with no outgoing flights in the data) are nodes too.
            allAirports.add(flight.getOrigin());
            allAirports.add(flight.getDestination());
        }

        // Sort once at build time by real instant; List.copyOf freezes each list so callers can't mutate it.
        Map<String, List<Flight>> sorted = new HashMap<>();
        for (Map.Entry<String, List<Flight>> entry : byOrigin.entrySet()) {
            List<Flight> departures = entry.getValue();
            departures.sort(Comparator.comparing(FlightNetwork::departureOf));
            sorted.put(entry.getKey(), List.copyOf(departures));
        }
        this.departuresByAirport = Map.copyOf(sorted);
        this.airports = Set.copyOf(allAirports);
    }

    /** All flights leaving {@code airport}, ordered by departure instant. Empty list for unknown airports. */
    public List<Flight> departuresFrom(String airport) {
        return departuresByAirport.getOrDefault(airport, List.of());
    }

    /**
     * Flights leaving {@code airport} with departure instant in [{@code from}, {@code to}] (both inclusive),
     * ordered by departure instant. Bonus: the list is sorted, so this can be a binary search.
     */
    public List<Flight> departuresBetween(String airport, Instant from, Instant to) {
        List<Flight> departures = departuresFrom(airport);
        int start = firstDepartingAtOrAfter(departures, from);
        int end = start;
        while (end < departures.size() && !departureOf(departures.get(end)).isAfter(to)) {
            end++;
        }
        // subList is a view of an immutable list, so it is unmodifiable too.
        return departures.subList(start, end);
    }

    /** Binary search ("lower bound"): index of the first flight departing at or after {@code instant}. */
    private static int firstDepartingAtOrAfter(List<Flight> departures, Instant instant) {
        int low = 0;
        int high = departures.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (departureOf(departures.get(mid)).isBefore(instant)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private static Instant departureOf(Flight flight) {
        return flight.getDepartureTime().toInstant();
    }

    /** Every airport that appears as origin or destination of at least one flight. */
    public Set<String> airports() {
        return airports;
    }
}
