package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.dto.CreateAirportRequest;
import com.graphsearch.FlightGraph.exception.AirportAlreadyExistsException;
import com.graphsearch.FlightGraph.exception.AirportNotFoundException;
import com.graphsearch.FlightGraph.model.Airport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AirportService {

    // Starts empty: filled by MockDataLoader at startup.
    private final List<Airport> airports = new ArrayList<>();

    public List<Airport> getAllAirports() {
        return airports;
    }

    /** @throws AirportNotFoundException if no airport has this code */
    public Airport getAirportByCode(String code) {
        return findByCode(code).orElseThrow(() -> new AirportNotFoundException(code));
    }

    /** Lookup without the HTTP-flavoured "not found" semantics, for callers that decide what a miss means. */
    public Optional<Airport> findByCode(String code) {
        return airports.stream().filter(a -> a.getCode().equalsIgnoreCase(code)).findFirst();
    }

    /**
     * Creates an airport from a request that already passed format validation (@Valid).
     * Here we only check rules that need existing data.
     *
     * @throws AirportAlreadyExistsException if the code is taken
     */
    public Airport createAirport(CreateAirportRequest request) {
        if (findByCode(request.code()).isPresent()) {
            throw new AirportAlreadyExistsException(request.code());
        }
        Airport airport = new Airport(request.code(), request.name(), request.city(), request.country(),
                request.latitude(), request.longitude(), request.timeZoneId());
        airports.add(airport);
        return airport;
    }

    /** Bulk insert for trusted sources (the mock loader); skips request validation on purpose. */
    public void addAll(List<Airport> newAirports) {
        airports.addAll(newAirports);
    }
}
