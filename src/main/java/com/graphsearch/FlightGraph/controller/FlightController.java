package com.graphsearch.FlightGraph.controller;

import java.time.LocalDate;
import java.util.List;
import com.graphsearch.FlightGraph.dto.CreateFlightRequest;
import com.graphsearch.FlightGraph.model.Flight;
import com.graphsearch.FlightGraph.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    // GET /flights?from=LIS&to=FRA&date=2027-05-10  (all parameters are optional)
    @GetMapping
    public List<Flight> getFlights(@RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) LocalDate date) {
        return flightService.search(from, to, date);
    }

    @GetMapping("/{id}")
    public Flight getFlight(@PathVariable String id) {
        return flightService.getFlightById(id);
    }

    /**
     * {@code @Valid} runs Bean Validation on the body before this method executes; on failure Spring
     * throws MethodArgumentNotValidException, which GlobalExceptionHandler turns into a 400.
     * Returns 201 Created + a Location header pointing to the new resource (REST convention).
     */
    @PostMapping
    public ResponseEntity<Flight> createFlight(@Valid @RequestBody CreateFlightRequest request) {
        Flight flight = flightService.createFlight(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(flight.getId()).toUri();
        return ResponseEntity.created(location).body(flight);
    }
}