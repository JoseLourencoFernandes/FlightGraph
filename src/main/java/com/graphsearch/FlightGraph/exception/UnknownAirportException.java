package com.graphsearch.FlightGraph.exception;

/**
 * An airport referenced inside a request body doesn't exist.
 * Not AirportNotFoundException: that one means "the URL you asked for doesn't exist" (404),
 * whereas here the URL (/flights) is fine — it's the content that is invalid.
 */
public class UnknownAirportException extends BusinessRuleException {

    public UnknownAirportException(String field, String code) {
        super(field + " references an unknown airport: " + code);
    }
}
