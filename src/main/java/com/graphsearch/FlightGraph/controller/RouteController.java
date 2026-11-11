package com.graphsearch.FlightGraph.controller;

import com.graphsearch.FlightGraph.service.RouteService;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /routes — ROADMAP step 2.2. The expected HTTP contract is in RouteControllerTest.
 *
 * TODO 2.2: add the endpoint, request-param validation and a response DTO (e.g. dto/RouteResponse)
 * instead of exposing the internal Itinerary record directly.
 */
@RestController
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }
}
