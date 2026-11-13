package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.Duration;
import java.util.Objects;

/**
 * When can a passenger connect from one flight to the next.
 *
 * @param minConnectionTime shortest allowed layover (inclusive), e.g. 45 min
 * @param maxLayover        longest allowed layover (inclusive), e.g. 24 h
 */
public record ConnectionRules(Duration minConnectionTime, Duration maxLayover) {

    public static final ConnectionRules DEFAULT = new ConnectionRules(Duration.ofMinutes(45), Duration.ofHours(24));

    public ConnectionRules {
        Objects.requireNonNull(minConnectionTime, "minConnectionTime");
        Objects.requireNonNull(maxLayover, "maxLayover");
        if (minConnectionTime.isNegative()) {
            throw new IllegalArgumentException("minConnectionTime must not be negative");
        }
        if (maxLayover.compareTo(minConnectionTime) < 0) {
            throw new IllegalArgumentException("maxLayover must be >= minConnectionTime");
        }
    }

    /**
     * True if a passenger arriving on {@code arriving} can board {@code departing}: same airport, and
     * the layover (between real instants!) is within [minConnectionTime, maxLayover].
     */
    public boolean allows(Flight arriving, Flight departing) {
        if (!arriving.getDestination().equals(departing.getOrigin())) {
            return false;
        }
        // Duration.between on OffsetDateTimes uses the real instants, so different offsets are handled.
        Duration layover = Duration.between(arriving.getArrivalTime(), departing.getDepartureTime());
        return layover.compareTo(minConnectionTime) >= 0 && layover.compareTo(maxLayover) <= 0;
    }
}
