package com.graphsearch.FlightGraph.controller;

import java.util.List;
import com.graphsearch.FlightGraph.dto.CreateAirportRequest;
import com.graphsearch.FlightGraph.model.Airport;
import com.graphsearch.FlightGraph.service.AirportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
        return airportService.getAirportByCode(code);
    }

    /** See FlightController#createFlight for how @Valid and 201 Created work. */
    @PostMapping
    public ResponseEntity<Airport> createAirport(@Valid @RequestBody CreateAirportRequest request) {
        Airport airport = airportService.createAirport(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}").buildAndExpand(airport.getCode()).toUri();
        return ResponseEntity.created(location).body(airport);
    }
}