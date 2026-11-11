package com.graphsearch.FlightGraph.routing;

import java.util.List;

/**
 * Enumerates every valid direct and one-stop itinerary — ROADMAP step 2.2.
 *
 * Deliberately simple (nested loops / depth-limited DFS). It is also the reference the
 * smarter 2.3 searches are compared against.
 */
public class OneStopRouteFinder {

    public OneStopRouteFinder(FlightNetwork network, ConnectionRules rules) {
        // TODO 2.2
    }

    /**
     * All valid itineraries for the query, in no particular order.
     * Valid = first leg departs on query.date() (local), connections respect the rules,
     * no airport visited twice, at most query.maxStops() stops.
     *
     * @throws IllegalArgumentException if query.maxStops() > 1 (use the 2.3 searches for that)
     */
    public List<Itinerary> findAll(RouteQuery query) {
        throw new UnsupportedOperationException("TODO 2.2");
    }
}
