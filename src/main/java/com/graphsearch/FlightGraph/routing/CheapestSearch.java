package com.graphsearch.FlightGraph.routing;

import java.util.Optional;

/**
 * Itinerary with the lowest total price — ROADMAP step 2.3.
 *
 * Flights with an unknown price (null) are never used: an itinerary with an unknown total
 * can't be claimed to be the cheapest. Ties may be broken any way.
 *
 * Careful: the cheapest way to reach an intermediate airport may arrive too late for the
 * connections you need — "cheapest so far at airport X" alone is not enough state.
 */
public class CheapestSearch implements BestItinerarySearch {

    public CheapestSearch(FlightNetwork network, ConnectionRules rules) {
        // TODO 2.3
    }

    @Override
    public Optional<Itinerary> find(RouteQuery query) {
        throw new UnsupportedOperationException("TODO 2.3");
    }
}
