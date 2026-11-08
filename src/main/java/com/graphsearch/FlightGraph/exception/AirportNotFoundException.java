package com.graphsearch.FlightGraph.exception;

public class AirportNotFoundException extends NotFoundException {

    public AirportNotFoundException(String code) {
        super("Airport not found: " + code);
    }
}
