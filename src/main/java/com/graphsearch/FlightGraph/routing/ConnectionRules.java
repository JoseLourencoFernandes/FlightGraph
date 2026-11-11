package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.Duration;

/**
 * When can a passenger connect from one flight to the next — ROADMAP step 2.1.
 *
 * @param minConnectionTime shortest allowed layover (inclusive), e.g. 45 min
 * @param maxLayover        longest allowed layover (inclusive), e.g. 24 h
 */
public record ConnectionRules(Duration minConnectionTime, Duration maxLayover) {

    public static final ConnectionRules DEFAULT = new ConnectionRules(Duration.ofMinutes(45), Duration.ofHours(24));

    public ConnectionRules {
        // TODO 2.1: reject nulls, a negative minConnectionTime and maxLayover < minConnectionTime
        //           (IllegalArgumentException)
    }

    /**
     * True if a passenger arriving on {@code arriving} can board {@code departing}: same airport, and
     * the layover (between real instants!) is within [minConnectionTime, maxLayover].
     */
    public boolean allows(Flight arriving, Flight departing) {
        throw new UnsupportedOperationException("TODO 2.1");
    }
}
