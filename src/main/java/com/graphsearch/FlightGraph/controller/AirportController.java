package com.graphsearch.FlightGraph.controller;

import java.util.List;
import com.graphsearch.FlightGraph.model.Airport;
import com.graphsearch.FlightGraph.service.AirportService;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @GetMapping
    public List<Airport> getAirports() {
        return airportService.getAllAirports();
    }

    @GetMapping("/{code}")
    public Airport getAirport(@PathVariable String code) {
        return airportService.getAirportByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport not found: " + code));
    }

    @PostMapping
    public Airport createAirport(@RequestBody Airport airport) {
        return airportService.addAirport(airport);
    }
}