package com.graphsearch.FlightGraph.exception;

public class AirportAlreadyExistsException extends ConflictException {

    public AirportAlreadyExistsException(String code) {
        super("Airport already exists: " + code);
    }
}
