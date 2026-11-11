package com.graphsearch.FlightGraph.routing;

/** How /routes results are ordered — ROADMAP steps 2.2/2.3. */
public enum RouteSort {
    /** Earliest arrival first. */
    ARRIVAL,
    /** Lowest total price first; itineraries with unknown price go last. */
    PRICE
}
