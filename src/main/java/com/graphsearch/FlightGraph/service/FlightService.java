package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.model.Flight;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FlightService {

    private final List<Flight> flights = new ArrayList<>(List.of(
            new Flight("1", "LIS", "MAD", OffsetDateTime.parse("2027-05-10T08:30+01:00"), OffsetDateTime.parse("2027-05-10T11:00+02:00"), 45.0, "Iberia"),
            new Flight("2", "MAD", "DOH", OffsetDateTime.parse("2027-05-10T14:00+02:00"), OffsetDateTime.parse("2027-05-10T21:00+03:00"), 320.0, "Qatar Airways"),
            new Flight("3", "DOH", "NRT", OffsetDateTime.parse("2027-05-11T02:00+03:00"), OffsetDateTime.parse("2027-05-11T17:45+09:00"), 410.0, "Qatar Airways")
    ));

    public List<Flight> getAllFlights() {
        return flights;
    }

    public Optional<Flight> getFlightById(String id) {
        return flights.stream().filter(f -> f.getId().equals(id)).findFirst();
    }

    public Flight addFlight(Flight flight) {
        flights.add(flight);
        return flight;
    }
}