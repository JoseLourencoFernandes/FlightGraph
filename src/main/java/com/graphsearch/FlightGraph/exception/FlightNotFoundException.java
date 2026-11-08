package com.graphsearch.FlightGraph.exception;

public class FlightNotFoundException extends NotFoundException {

    public FlightNotFoundException(String id) {
        super("Flight not found: " + id);
    }
}
