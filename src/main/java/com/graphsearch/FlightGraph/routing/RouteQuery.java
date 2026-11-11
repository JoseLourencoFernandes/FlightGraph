package com.graphsearch.FlightGraph.routing;

import java.time.LocalDate;
import java.util.Objects;

/**
 * What the user is looking for.
 *
 * @param date     the FIRST leg must depart on this LOCAL date at the origin airport, i.e.
 *                 {@code flight.getDepartureTime().toLocalDate().equals(date)} (flight times are stored
 *                 in their airport's offset). Later legs may depart on following days.
 * @param maxStops maximum number of connections (0 = direct only)
 */
public record RouteQuery(String origin, String destination, LocalDate date, int maxStops) {

    public RouteQuery {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(date, "date");
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("origin and destination must be different");
        }
        if (maxStops < 0) {
            throw new IllegalArgumentException("maxStops must be >= 0");
        }
    }
}
