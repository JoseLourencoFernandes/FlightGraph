package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.model.Airport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AirportService{

    private final List<Airport> airports = new ArrayList<>(List.of(
            new Airport("LIS", "Lisbon Airport", "Lisbon", "Portugal"),
            new Airport("MAD", "Madrid-Barajas Airport", "Madrid", "Spain"),
            new Airport("DOH", "Doha", "Doha", "Qatar"),
            new Airport("NRT", "Tokyo Narita Airport", "Tokyo", "Japan")
    ));

    public List<Airport> getAllAirports() {
        return airports;
    }

    public Optional<Airport> getAirportByCode(String code) {
        return airports.stream().filter(a -> a.getCode().equals(code)).findFirst();
    }

    public Airport addAirport(Airport airport) {
        airports.add(airport);
        return airport;
    }
}