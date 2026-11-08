package com.graphsearch.FlightGraph.exception;

/**
 * Domain exception: "the thing you asked for doesn't exist".
 *
 * Services throw this without knowing anything about HTTP; GlobalExceptionHandler is the
 * only place that maps it to a 404. Subclasses give each resource its own type and message.
 */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
