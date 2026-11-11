package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The flight network as a directed, temporal graph — ROADMAP step 2.1.
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

    private final Map<String, List<Flight>> graph;

    public FlightNetwork(Collection<Flight> flights) {
        this.graph = new HashMap<>();
        for (Flight flight : flights) {
            addFlight(flight);
        }
    }

    private void addFlight(Flight flight) {
        graph.computeIfAbsent(flight.getOrigin(), k -> new ArrayList<>()).add(flight);
    }


    /** All flights leaving {@code airport}, ordered by departure instant. Empty list for unknown airports. */
    public List<Flight> departuresFrom(String airport) {
        return Collections.unmodifiableList(graph.getOrDefault(airport, new ArrayList<>()));
    }

    /**
     * Flights leaving {@code airport} with departure instant in [{@code from}, {@code to}] (both inclusive),
     * ordered by departure instant. Bonus: the list is sorted, so this can be a binary search.
     */
    public List<Flight> departuresBetween(String airport, Instant from, Instant to) {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    /** Every airport that appears as origin or destination of at least one flight. */
    public Set<String> airports() {
        throw new UnsupportedOperationException("TODO 2.1");
    }
}
