package com.graphsearch.FlightGraph.exception;

/**
 * Domain exception: the request is well-formed but breaks a business rule
 * (e.g. a flight referencing an airport that doesn't exist). Mapped to 422.
 */
public abstract class BusinessRuleException extends RuntimeException {

    protected BusinessRuleException(String message) {
        super(message);
    }
}
