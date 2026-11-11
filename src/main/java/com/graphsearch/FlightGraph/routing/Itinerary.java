package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A journey made of one or more chained flights ("legs") — ROADMAP step 2.1.
 *
 * The constructor only checks STRUCTURE (non-empty, legs chained, no overlap). Whether a layover is
 * long enough is a ConnectionRules concern, checked by the search algorithms.
 */
public record Itinerary(List<Flight> legs) {

    public Itinerary {
        // TODO 2.1:
        //  - reject null/empty legs
        //  - each leg must start where the previous one ended (destination == next origin)
        //  - each leg must depart at or after the previous one arrives
        //  (IllegalArgumentException), and store an immutable copy of the list
    }

    public String origin() {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    public String destination() {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    /** Departure of the first leg. */
    public OffsetDateTime departureTime() {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    /** Arrival of the last leg. */
    public OffsetDateTime arrivalTime() {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    /** Number of connections: a direct flight has 0 stops. */
    public int stops() {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    /** From first departure to last arrival (real elapsed time, time zones don't matter). */
    public Duration totalDuration() {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    /** Time on the ground between consecutive legs; empty for a direct flight. */
    public List<Duration> layovers() {
        throw new UnsupportedOperationException("TODO 2.1");
    }

    /** Sum of leg prices, or {@code null} if ANY leg has an unknown price (never treat unknown as 0). */
    public Double totalPrice() {
        throw new UnsupportedOperationException("TODO 2.1");
    }
}
