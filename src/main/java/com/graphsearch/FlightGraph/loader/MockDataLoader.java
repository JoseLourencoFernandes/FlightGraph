package com.graphsearch.FlightGraph.loader;

import com.graphsearch.FlightGraph.loader.MockJson.*;
import com.graphsearch.FlightGraph.model.Airport;
import com.graphsearch.FlightGraph.model.Flight;
import com.graphsearch.FlightGraph.service.AirportService;
import com.graphsearch.FlightGraph.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads the files in src/main/resources/mock-data when the application starts.
 *
 * An ApplicationRunner is a bean whose run() Spring calls once, after the context is ready
 * (all beans created). That makes it the right place to seed data.
 */
@Component
public class MockDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataLoader.class);

    private final ObjectMapper objectMapper;   // auto-configured by Spring Boot
    private final AirportService airportService;
    private final FlightService flightService;

    public MockDataLoader(ObjectMapper objectMapper, AirportService airportService, FlightService flightService) {
        this.objectMapper = objectMapper;
        this.airportService = airportService;
        this.flightService = flightService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<Airport> airports = loadAirports();
        airportService.addAll(airports);

        // Each airport's time zone is needed to turn local times into real instants.
        Map<String, ZoneId> zones = airports.stream()
                .collect(Collectors.toMap(Airport::getCode, a -> ZoneId.of(a.getTimeZoneId())));

        Map<String, Double> prices = loadPrices();
        List<Flight> flights = loadFlights(zones, prices);
        flightService.addAll(flights);

        long priced = flights.stream().filter(f -> f.getPrice() != null).count();
        log.info("Mock data loaded: {} airports, {} flights ({} with price)", airports.size(), flights.size(), priced);
    }

    // ---------------------------------------------------------------- airports

    private List<Airport> loadAirports() throws IOException {
        AirportsFile file = read("mock-data/airports.json", AirportsFile.class);
        return file.airportResource().airports().airport().stream()
                .map(this::toAirport)
                .toList();
    }

    private Airport toAirport(AirportJson a) {
        String name = a.names().name().stream()
                .filter(n -> "EN".equals(n.languageCode()))
                .map(Name::value)
                .findFirst()
                .orElse(a.airportCode());
        Coordinate c = a.position().coordinate();
        return new Airport(a.airportCode(), name, a.cityCode(), a.countryCode(),
                c.latitude(), c.longitude(), a.timeZoneId());
    }

    // ---------------------------------------------------------------- prices

    /** Key: "LH1139|2027-05-10" -> price. A Map gives O(1) lookups while building flights. */
    private Map<String, Double> loadPrices() throws IOException {
        PricesFile file = read("mock-data/prices.json", PricesFile.class);
        return file.prices().stream()
                .collect(Collectors.toMap(
                        p -> priceKey(p.carrier(), p.flightNumber(), LocalDate.parse(p.departureDate())),
                        PriceJson::amount,
                        (a, b) -> a)); // on duplicates, keep the first
    }

    private static String priceKey(String carrier, String flightNumber, LocalDate date) {
        return carrier + flightNumber + "|" + date;
    }

    // ---------------------------------------------------------------- flights

    private List<Flight> loadFlights(Map<String, ZoneId> zones, Map<String, Double> prices) throws IOException {
        SchedulesFile file = read("mock-data/schedules.json", SchedulesFile.class);
        List<Flight> flights = new ArrayList<>();
        for (Route route : file.routes()) {
            for (Schedule schedule : route.scheduleResource().schedule()) {
                flights.addAll(expand(schedule, zones, prices));
            }
        }
        flights.sort(Comparator.comparing(Flight::getDepartureTime));
        return flights;
    }

    /**
     * A Schedule is NOT a flight: it is a pattern ("departs 07:45, on days 12345, May 1–31").
     * Here it is expanded into concrete flights, one per operating day.
     */
    private List<Flight> expand(Schedule schedule, Map<String, ZoneId> zones, Map<String, Double> prices) {
        ScheduledFlight f = schedule.flight();
        String origin = f.departure().airportCode();
        String destination = f.arrival().airportCode();
        String carrier = f.marketingCarrier().airlineId();
        String number = f.marketingCarrier().flightNumber();

        LocalTime departureLocalTime = LocalDateTime.parse(f.departure().scheduledTimeLocal().dateTime()).toLocalTime();
        Duration duration = Duration.parse(schedule.totalJourney().duration());
        String days = f.details().daysOfOperation();
        LocalDate from = LocalDate.parse(f.details().datePeriod().effective());
        LocalDate to = LocalDate.parse(f.details().datePeriod().expiration());

        ZoneId originZone = zones.get(origin);
        ZoneId destinationZone = zones.get(destination);

        List<Flight> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            // DayOfWeek.getValue(): 1 = Monday ... 7 = Sunday — same convention as DaysOfOperation
            if (days.indexOf(Character.forDigit(date.getDayOfWeek().getValue(), 10)) < 0) {
                continue;
            }
            // Local time + origin time zone -> real instant (offset-aware, handles DST)
            ZonedDateTime departure = ZonedDateTime.of(date, departureLocalTime, originZone);
            // Arrival = departure + duration, expressed in the destination's time zone
            ZonedDateTime arrival = departure.plus(duration).withZoneSameInstant(destinationZone);

            result.add(new Flight(
                    carrier + number + "-" + date,       // unique id, e.g. "LH1139-2027-05-10"
                    origin,
                    destination,
                    departure.toOffsetDateTime(),
                    arrival.toOffsetDateTime(),
                    prices.get(priceKey(carrier, number, date)), // null if no price
                    carrier,
                    number));
        }
        return result;
    }

    // ---------------------------------------------------------------- util

    private <T> T read(String path, Class<T> type) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        }
    }
}
