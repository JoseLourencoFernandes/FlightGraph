package com.graphsearch.FlightGraph.routing;

import java.util.Optional;

/**
 * Finds the single best itinerary by some criterion, for any number of stops — ROADMAP step 2.3.
 * Implementations decide what "best" means (earliest arrival, lowest price...).
 */
public interface BestItinerarySearch {

    /** The best valid itinerary for the query (same validity rules as OneStopRouteFinder), or empty. */
    Optional<Itinerary> find(RouteQuery query);
}
