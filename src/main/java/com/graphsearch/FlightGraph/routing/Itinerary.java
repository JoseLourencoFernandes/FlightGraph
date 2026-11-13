package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A journey made of one or more chained flights ("legs").
 *
 * The constructor only checks STRUCTURE (non-empty, legs chained, no overlap). Whether a layover is
 * long enough is a ConnectionRules concern, checked by the search algorithms.
 */
public record Itinerary(List<Flight> legs) {

    public Itinerary {
        if (legs == null || legs.isEmpty()) {
            throw new IllegalArgumentException("an itinerary needs at least one leg");
        }
        // Defensive copy: the caller's list can change later, the itinerary must not.
        legs = List.copyOf(legs);
        for (int i = 1; i < legs.size(); i++) {
            Flight previous = legs.get(i - 1);
            Flight next = legs.get(i);
            if (!previous.getDestination().equals(next.getOrigin())) {
                throw new IllegalArgumentException("leg " + next.getId() + " does not depart from "
                        + previous.getDestination());
            }
            if (next.getDepartureTime().isBefore(previous.getArrivalTime())) {
                throw new IllegalArgumentException("leg " + next.getId() + " departs before "
                        + previous.getId() + " arrives");
            }
        }
    }

    public String origin() {
        return first().getOrigin();
    }

    public String destination() {
        return last().getDestination();
    }

    /** Departure of the first leg. */
    public OffsetDateTime departureTime() {
        return first().getDepartureTime();
    }

    /** Arrival of the last leg. */
    public OffsetDateTime arrivalTime() {
        return last().getArrivalTime();
    }

    /** Number of connections: a direct flight has 0 stops. */
    public int stops() {
        return legs.size() - 1;
    }

    /** From first departure to last arrival (real elapsed time, time zones don't matter). */
    public Duration totalDuration() {
        return Duration.between(departureTime(), arrivalTime());
    }

    /** Time on the ground between consecutive legs; empty for a direct flight. */
    public List<Duration> layovers() {
        List<Duration> layovers = new ArrayList<>();
        for (int i = 1; i < legs.size(); i++) {
            layovers.add(Duration.between(legs.get(i - 1).getArrivalTime(), legs.get(i).getDepartureTime()));
        }
        return List.copyOf(layovers);
    }

    /** Sum of leg prices, or {@code null} if ANY leg has an unknown price (never treat unknown as 0). */
    public Double totalPrice() {
        double total = 0;
        for (Flight leg : legs) {
            if (leg.getPrice() == null) {
                return null;
            }
            total += leg.getPrice();
        }
        return total;
    }

    // Java 17 has no List.getFirst()/getLast() (added in 21).
    private Flight first() {
        return legs.get(0);
    }

    private Flight last() {
        return legs.get(legs.size() - 1);
    }
}
