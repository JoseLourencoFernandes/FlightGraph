package com.graphsearch.FlightGraph.routing;

import java.util.Optional;

/**
 * Itinerary that reaches the destination as early as possible — ROADMAP step 2.3.
 *
 * Suggested approach: time-dependent Dijkstra with a PriorityQueue ordered by arrival instant.
 * Careful: with a maxStops limit, "earliest arrival at an airport" alone is not enough state.
 * Ties (same arrival instant) may be broken any way.
 */
public class EarliestArrivalSearch implements BestItinerarySearch {

    public EarliestArrivalSearch(FlightNetwork network, ConnectionRules rules) {
        // TODO 2.3
    }

    @Override
    public Optional<Itinerary> find(RouteQuery query) {
        throw new UnsupportedOperationException("TODO 2.3");
    }
}
