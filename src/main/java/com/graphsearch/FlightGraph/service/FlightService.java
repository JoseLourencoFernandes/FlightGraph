package com.graphsearch.FlightGraph.service;

import com.graphsearch.FlightGraph.dto.CreateFlightRequest;
import com.graphsearch.FlightGraph.exception.FlightAlreadyExistsException;
import com.graphsearch.FlightGraph.exception.FlightNotFoundException;
import com.graphsearch.FlightGraph.exception.UnknownAirportException;
import com.graphsearch.FlightGraph.model.Airport;
import com.graphsearch.FlightGraph.model.Flight;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class FlightService {

    private final AirportService airportService;

    // Starts empty: filled by MockDataLoader at startup.
    private final List<Flight> flights = new ArrayList<>();

    public FlightService(AirportService airportService) {
        this.airportService = airportService;
    }

    public List<Flight> getAllFlights() {
        return flights;
    }

    /**
     * Filters flights; any null parameter is ignored.
     * The date is compared with the LOCAL departure date (origin airport's time zone),
     * which is what a person means by "flights from Lisbon on the 10th".
     */
    public List<Flight> search(String from, String to, LocalDate date) {
        return flights.stream()
                .filter(f -> from == null || f.getOrigin().equalsIgnoreCase(from))
                .filter(f -> to == null || f.getDestination().equalsIgnoreCase(to))
                .filter(f -> date == null || f.getDepartureTime().toLocalDate().equals(date))
                .toList();
    }

    /** @throws FlightNotFoundException if no flight has this id */
    public Flight getFlightById(String id) {
        return flights.stream()
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new FlightNotFoundException(id));
    }

    /**
     * Creates a flight from a request that already passed format validation (@Valid).
     *
     * @throws UnknownAirportException      if origin or destination doesn't exist
     * @throws FlightAlreadyExistsException if the same flight number already departs that day
     */
    public Flight createFlight(CreateFlightRequest request) {
        Airport origin = airportService.findByCode(request.origin())
                .orElseThrow(() -> new UnknownAirportException("origin", request.origin()));
        Airport destination = airportService.findByCode(request.destination())
                .orElseThrow(() -> new UnknownAirportException("destination", request.destination()));

        // Normalize offsets to each airport's local zone, same as the loader does. The client may send
        // "2027-05-10T23:30Z" for a Lisbon flight; search() relies on departureTime.toLocalDate() being
        // the date as seen at the origin airport, so the offset must be the origin's.
        OffsetDateTime departure = atZone(request.departureTime(), origin);
        OffsetDateTime arrival = atZone(request.arrivalTime(), destination);

        // Same id scheme as the mock data: carrier + number + local departure date, e.g. "LH1139-2027-05-10"
        String id = request.carrier() + request.flightNumber() + "-" + departure.toLocalDate();
        if (flights.stream().anyMatch(f -> f.getId().equals(id))) {
            throw new FlightAlreadyExistsException(id);
        }

        Flight flight = new Flight(id, origin.getCode(), destination.getCode(), departure, arrival,
                request.price(), request.carrier(), request.flightNumber());
        flights.add(flight);
        return flight;
    }

    /** Bulk insert for trusted sources (the mock loader); skips request validation on purpose. */
    public void addAll(List<Flight> newFlights) {
        flights.addAll(newFlights);
    }

    private static OffsetDateTime atZone(OffsetDateTime time, Airport airport) {
        return time.atZoneSameInstant(ZoneId.of(airport.getTimeZoneId())).toOffsetDateTime();
    }
}
