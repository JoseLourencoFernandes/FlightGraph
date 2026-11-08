package com.graphsearch.FlightGraph.exception;

/** Domain exception: the request clashes with existing data (e.g. duplicate key). Mapped to 409. */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
