package com.graphsearch.FlightGraph.controller;

import java.util.List;
import com.graphsearch.FlightGraph.model.Flight;
import com.graphsearch.FlightGraph.service.FlightService;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public List<Flight> getFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{id}")
    public Flight getFlight(@PathVariable String id) {
        return flightService.getFlightById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found: " + id));
    }

    @PostMapping
    public Flight createFlight(@RequestBody Flight flight) {
        return flightService.addFlight(flight);
    }
}