package com.graphsearch.FlightGraph.exception;

public class FlightAlreadyExistsException extends ConflictException {

    public FlightAlreadyExistsException(String id) {
        super("Flight already exists: " + id);
    }
}
