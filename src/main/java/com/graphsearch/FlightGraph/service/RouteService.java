package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.routing.Itinerary;
import com.graphsearch.FlightGraph.routing.RouteSort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Bridges the web layer and the routing engine — ROADMAP steps 2.2/2.3.
 * Gets the current flights from FlightService, builds a FlightNetwork and runs the right search.
 */
@Service
public class RouteService {

    private final FlightService flightService;

    public RouteService(FlightService flightService) {
        this.flightService = flightService;
    }

    /**
     * Itineraries from {@code from} to {@code to} whose first leg departs on {@code date}.
     *
     * - maxStops 0 or 1 (2.2): ALL valid itineraries, ordered by {@code sort}.
     * - maxStops >= 2 (2.3): the first element is the best one for {@code sort}
     *   (earliest arrival / lowest price); returning more results after it is up to you.
     *
     * PRICE order puts itineraries with an unknown total price last.
     */
    public List<Itinerary> findRoutes(String from, String to, LocalDate date, int maxStops, RouteSort sort) {
        throw new UnsupportedOperationException("TODO 2.2 / 2.3");
    }
}
