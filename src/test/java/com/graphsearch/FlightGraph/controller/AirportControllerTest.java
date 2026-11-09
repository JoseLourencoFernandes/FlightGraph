package com.graphsearch.FlightGraph.controller;

import com.graphsearch.FlightGraph.exception.AirportAlreadyExistsException;
import com.graphsearch.FlightGraph.exception.AirportNotFoundException;
import com.graphsearch.FlightGraph.model.Airport;
import com.graphsearch.FlightGraph.service.AirportService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/** Web-layer tests for /airports. See FlightControllerTest for how @WebMvcTest + @MockitoBean work. */
@WebMvcTest(AirportController.class)
class AirportControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AirportService airportService;

    private static final Airport PORTO = new Airport("OPO", "Porto", "OPO", "PT", 41.24, -8.68, "Europe/Lisbon");

    @Nested
    class Read {

        @Test
        void shouldListAirports() {
            given(airportService.getAllAirports()).willReturn(List.of(PORTO));

            assertThat(mvc.get().uri("/airports"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].code").asArray().containsExactly("OPO");
        }

        @Test
        void shouldReturn404ProblemDetailWhenAirportDoesNotExist() {
            given(airportService.getAirportByCode("XYZ")).willThrow(new AirportNotFoundException("XYZ"));

            assertThat(mvc.get().uri("/airports/XYZ"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().extractingPath("$.detail").isEqualTo("Airport not found: XYZ");
        }
    }

    @Nested
    class CreateAirport {

        @Test
        void shouldReturn201WithLocationHeader() {
            given(airportService.createAirport(any())).willReturn(PORTO);

            assertThat(mvc.post().uri("/airports").contentType(MediaType.APPLICATION_JSON)
                    .content(body("OPO", "PT", "41.24", "-8.68", "Europe/Lisbon")))
                    .hasStatus(HttpStatus.CREATED)
                    .hasHeader("Location", "http://localhost/airports/OPO");
        }

        @Test
        void shouldReturn400WhenCoordinatesAreMissing() {
            // Guards the Double-vs-double choice in the DTO: a missing field must not become 0.0
            String body = """
                    { "code": "OPO", "name": "Porto", "city": "OPO", "country": "PT", "timeZoneId": "Europe/Lisbon" }
                    """;

            assertThat(mvc.post().uri("/airports").contentType(MediaType.APPLICATION_JSON).content(body))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.errors").asMap().containsOnlyKeys("latitude", "longitude");

            verifyNoInteractions(airportService);
        }

        // One row per invalid input: field value(s) + the error key we expect in the response.
        @ParameterizedTest(name = "{5} rejected")
        @CsvSource({
                "op,  PT,  41.24, -8.68,  Europe/Lisbon,  code",
                "OPO, PRT, 41.24, -8.68,  Europe/Lisbon,  country",
                "OPO, PT,  91,    -8.68,  Europe/Lisbon,  latitude",
                "OPO, PT,  41.24, -181,   Europe/Lisbon,  longitude",
                "OPO, PT,  41.24, -8.68,  Europe/Nowhere, validTimeZoneId",
        })
        void shouldReturn400ForInvalidField(String code, String country, String lat, String lon,
                                            String timeZone, String expectedErrorField) {
            assertThat(mvc.post().uri("/airports").contentType(MediaType.APPLICATION_JSON)
                    .content(body(code, country, lat, lon, timeZone)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.errors").asMap().containsOnlyKeys(expectedErrorField);

            verifyNoInteractions(airportService);
        }

        @Test
        void shouldReturn409WhenAirportAlreadyExists() {
            given(airportService.createAirport(any())).willThrow(new AirportAlreadyExistsException("OPO"));

            assertThat(mvc.post().uri("/airports").contentType(MediaType.APPLICATION_JSON)
                    .content(body("OPO", "PT", "41.24", "-8.68", "Europe/Lisbon")))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().extractingPath("$.detail").isEqualTo("Airport already exists: OPO");
        }
    }

    private static String body(String code, String country, String lat, String lon, String timeZone) {
        return """
                { "code": "%s", "name": "Porto", "city": "OPO", "country": "%s",
                  "latitude": %s, "longitude": %s, "timeZoneId": "%s" }
                """.formatted(code, country, lat, lon, timeZone);
    }
}
