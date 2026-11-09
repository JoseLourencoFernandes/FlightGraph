package com.graphsearch.FlightGraph.controller;

import com.graphsearch.FlightGraph.dto.CreateFlightRequest;
import com.graphsearch.FlightGraph.exception.FlightAlreadyExistsException;
import com.graphsearch.FlightGraph.exception.FlightNotFoundException;
import com.graphsearch.FlightGraph.exception.UnknownAirportException;
import com.graphsearch.FlightGraph.model.Flight;
import com.graphsearch.FlightGraph.service.FlightService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Web-layer ("slice") tests. @WebMvcTest starts only the MVC infrastructure: this controller,
 * every @RestControllerAdvice (so GlobalExceptionHandler is included), Jackson and Bean Validation.
 * Services and MockDataLoader are NOT loaded — FlightService is replaced by a Mockito mock.
 *
 * What we check here is the HTTP contract: URLs, status codes, headers, JSON shape, validation and
 * error mapping. The business logic itself is covered by FlightServiceTest.
 */
@WebMvcTest(FlightController.class)
class FlightControllerTest {

    @Autowired
    private MockMvcTester mvc; // sends fake HTTP requests through Spring MVC, no real server/port

    @MockitoBean
    private FlightService flightService; // a mock registered as a bean, injected into the controller

    private static final Flight FLIGHT = new Flight("TP999-2027-05-10", "LIS", "FRA",
            OffsetDateTime.parse("2027-05-10T11:00+01:00"), OffsetDateTime.parse("2027-05-10T15:00+02:00"),
            99.5, "TP", "999");

    private static final String VALID_BODY = """
            {
              "origin": "LIS", "destination": "FRA",
              "departureTime": "2027-05-10T10:00:00Z", "arrivalTime": "2027-05-10T13:00:00Z",
              "price": 99.5, "carrier": "TP", "flightNumber": "999"
            }
            """;

    @Nested
    class SearchFlights {

        @Test
        void shouldPassQueryParamsToServiceAndReturnJsonArray() {
            given(flightService.search("LIS", "FRA", LocalDate.parse("2027-05-10"))).willReturn(List.of(FLIGHT));

            assertThat(mvc.get().uri("/flights?from=LIS&to=FRA&date=2027-05-10"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[0].id").isEqualTo("TP999-2027-05-10");
        }

        @Test
        void shouldTreatMissingParamsAsNull() {
            given(flightService.search(null, null, null)).willReturn(List.of());

            assertThat(mvc.get().uri("/flights")).hasStatusOk().bodyJson().isEqualTo("[]");
            verify(flightService).search(null, null, null);
        }

        @Test
        void shouldReturn400WhenDateIsInvalid() {
            assertThat(mvc.get().uri("/flights?date=abc"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);

            verifyNoInteractions(flightService); // Spring rejected it before reaching the controller
        }
    }

    @Nested
    class GetFlight {

        @Test
        void shouldReturnFlightAsJson() {
            given(flightService.getFlightById("TP999-2027-05-10")).willReturn(FLIGHT);

            assertThat(mvc.get().uri("/flights/{id}", "TP999-2027-05-10"))
                    .hasStatusOk()
                    .bodyJson()
                    // the JSON contract: field names and ISO-8601 times with offset
                    .isLenientlyEqualTo("""
                            {
                              "id": "TP999-2027-05-10", "origin": "LIS", "destination": "FRA",
                              "departureTime": "2027-05-10T11:00:00+01:00",
                              "arrivalTime": "2027-05-10T15:00:00+02:00",
                              "price": 99.5, "carrier": "TP", "flightNumber": "999"
                            }
                            """);
        }

        @Test
        void shouldReturn404ProblemDetailWhenFlightDoesNotExist() {
            given(flightService.getFlightById("NOPE")).willThrow(new FlightNotFoundException("NOPE"));

            assertThat(mvc.get().uri("/flights/NOPE"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isLenientlyEqualTo("""
                            { "status": 404, "title": "Not Found",
                              "detail": "Flight not found: NOPE", "instance": "/flights/NOPE" }
                            """);
        }
    }

    @Nested
    class CreateFlight {

        @Test
        void shouldReturn201WithLocationHeaderAndBody() {
            given(flightService.createFlight(any())).willReturn(FLIGHT);

            assertThat(mvc.post().uri("/flights").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .hasStatus(HttpStatus.CREATED)
                    .hasHeader("Location", "http://localhost/flights/TP999-2027-05-10")
                    .bodyJson().extractingPath("$.id").isEqualTo("TP999-2027-05-10");
        }

        @Test
        void shouldPassDeserializedRequestToService() {
            given(flightService.createFlight(any())).willReturn(FLIGHT);

            mvc.post().uri("/flights").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY).exchange();

            // Records have equals() for free, so we can compare the whole request in one go
            verify(flightService).createFlight(new CreateFlightRequest("LIS", "FRA",
                    OffsetDateTime.parse("2027-05-10T10:00:00Z"), OffsetDateTime.parse("2027-05-10T13:00:00Z"),
                    99.5, "TP", "999"));
        }

        @Test
        void shouldReturn400WithEveryMissingFieldWhenBodyIsEmpty() {
            assertThat(mvc.post().uri("/flights").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.errors").asMap().containsOnlyKeys(
                            "origin", "destination", "departureTime", "arrivalTime", "carrier", "flightNumber");

            verifyNoInteractions(flightService);
        }

        @Test
        void shouldReturn400WithMessagePerInvalidField() {
            String body = """
                    {
                      "origin": "lis", "destination": "FRA",
                      "departureTime": "2027-05-10T10:00:00Z", "arrivalTime": "2027-05-10T09:00:00Z",
                      "price": -5, "carrier": "LUFT", "flightNumber": "12a"
                    }
                    """;

            assertThat(mvc.post().uri("/flights").contentType(MediaType.APPLICATION_JSON).content(body))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isLenientlyEqualTo("""
                            { "detail": "Request validation failed",
                              "errors": {
                                "origin": ["must be a 3-letter IATA airport code"],
                                "price": ["must be greater than 0"],
                                "carrier": ["must be a 2-character IATA airline code"],
                                "flightNumber": ["must be 1 to 4 digits"],
                                "arrivalAfterDeparture": ["arrivalTime must be after departureTime"]
                              } }
                            """);

            verifyNoInteractions(flightService);
        }

        @Test
        void shouldReturn400WhenJsonIsMalformed() {
            assertThat(mvc.post().uri("/flights").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }

        @Test
        void shouldReturn422WhenServiceRejectsUnknownAirport() {
            given(flightService.createFlight(any())).willThrow(new UnknownAirportException("destination", "XYZ"));

            assertThat(mvc.post().uri("/flights").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                    .bodyJson().extractingPath("$.detail").isEqualTo("destination references an unknown airport: XYZ");
        }

        @Test
        void shouldReturn409WhenFlightAlreadyExists() {
            given(flightService.createFlight(any())).willThrow(new FlightAlreadyExistsException("TP999-2027-05-10"));

            assertThat(mvc.post().uri("/flights").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .hasStatus(HttpStatus.CONFLICT);
        }
    }

    @Nested
    class UnexpectedErrors {

        @Test
        void shouldReturnGeneric500WithoutLeakingInternals() {
            given(flightService.search(any(), any(), any()))
                    .willThrow(new IllegalStateException("db password is hunter2"));

            assertThat(mvc.get().uri("/flights"))
                    .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .bodyText().contains("Unexpected error").doesNotContain("hunter2");
        }

        @Test
        void shouldReturn405ForUnsupportedMethod() {
            assertThat(mvc.delete().uri("/flights"))
                    .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }
    }
}
