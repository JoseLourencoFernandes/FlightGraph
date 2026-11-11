package com.graphsearch.FlightGraph.controller;

import com.graphsearch.FlightGraph.routing.Itinerary;
import com.graphsearch.FlightGraph.routing.RouteSort;
import com.graphsearch.FlightGraph.service.RouteService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static com.graphsearch.FlightGraph.routing.RoutingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * ROADMAP 2.2 — the HTTP contract of GET /routes.
 *
 * GET /routes?from=LIS&to=NRT&date=2027-05-10[&maxStops=0..3 (default 1)][&sort=arrival|price (default arrival)]
 * → 200 with a JSON array; each element has: legs (the flights), stops, departureTime, arrivalTime,
 *   totalDurationMinutes, layoverMinutes, totalPrice (null when unknown).
 * Invalid input → 400 ProblemDetail, and the service is never called.
 */
@Disabled("ROADMAP 2.2 — delete this line when you build the /routes endpoint")
@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RouteService routeService;

    @Test
    void shouldUseDefaultsAndReturnItinerariesAsJson() {
        given(routeService.findRoutes("LIS", "NRT", DAY, 1, RouteSort.ARRIVAL))
                .willReturn(List.of(new Itinerary(List.of(LIS_FRA_0800, FRA_NRT_1200))));

        var response = assertThat(mvc.get().uri("/routes?from=LIS&to=NRT&date=2027-05-10")).hasStatusOk().bodyJson();

        response.extractingPath("$[0].legs[*].id").asArray().containsExactly("LIS-FRA-0800", "FRA-NRT-1200");
        response.extractingPath("$[0].stops").isEqualTo(1);
        response.extractingPath("$[0].totalDurationMinutes").isEqualTo(960);  // 08:00 -> 00:00 next day
        response.extractingPath("$[0].layoverMinutes").asArray().containsExactly(60);
        response.extractingPath("$[0].totalPrice").isEqualTo(550.0);
        response.extractingPath("$[0].departureTime").isNotNull();
        response.extractingPath("$[0].arrivalTime").isNotNull();
    }

    @Test
    void shouldReturnNullTotalPriceWhenUnknown() {
        given(routeService.findRoutes(any(), any(), any(), anyInt(), any()))
                .willReturn(List.of(new Itinerary(List.of(LIS_FRA_1500, FRA_NRT_2200))));

        assertThat(mvc.get().uri("/routes?from=LIS&to=NRT&date=2027-05-10"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[0].totalPrice").isNull();
    }

    @Test
    void shouldPassSortAndMaxStopsToService() {
        given(routeService.findRoutes(any(), any(), any(), anyInt(), any())).willReturn(List.of());

        assertThat(mvc.get().uri("/routes?from=LIS&to=NRT&date=2027-05-10&maxStops=3&sort=price")).hasStatusOk();

        verify(routeService).findRoutes("LIS", "NRT", DAY, 3, RouteSort.PRICE);
    }

    @Test
    void shouldReturnEmptyArrayWhenNoRoutes() {
        given(routeService.findRoutes(any(), any(), any(), anyInt(), any())).willReturn(List.of());

        assertThat(mvc.get().uri("/routes?from=LIS&to=NRT&date=2027-05-10"))
                .hasStatusOk().bodyJson().isEqualTo("[]");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/routes?to=NRT&date=2027-05-10",                        // missing from
            "/routes?from=LIS&date=2027-05-10",                      // missing to
            "/routes?from=LIS&to=NRT",                               // missing date
            "/routes?from=LIS&to=NRT&date=10-05-2027",               // bad date
            "/routes?from=LIS&to=LIS&date=2027-05-10",               // same airport
            "/routes?from=LIS&to=NRT&date=2027-05-10&maxStops=-1",   // negative
            "/routes?from=LIS&to=NRT&date=2027-05-10&maxStops=4",    // too many (max 3)
            "/routes?from=LIS&to=NRT&date=2027-05-10&sort=cheapest", // unknown sort
    })
    void shouldReturn400ProblemDetailForInvalidInput(String uri) {
        assertThat(mvc.get().uri(uri))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);

        verifyNoInteractions(routeService);
    }
}
