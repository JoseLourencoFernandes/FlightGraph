package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates small random networks for differential testing. Seeded, so a failing case is
 * reproducible: the test name shows the seed, and the same seed always gives the same network.
 */
final class RandomNetworks {

    static final String[] AIRPORTS = {"AAA", "BBB", "CCC", "DDD", "EEE", "FFF"};
    private static final OffsetDateTime START = OffsetDateTime.parse("2027-05-10T00:00Z");

    private RandomNetworks() {}

    static List<Flight> generate(long seed, int flightCount) {
        Random rng = new Random(seed);
        List<Flight> flights = new ArrayList<>();
        for (int i = 0; i < flightCount; i++) {
            String origin = AIRPORTS[rng.nextInt(AIRPORTS.length)];
            String destination;
            do {
                destination = AIRPORTS[rng.nextInt(AIRPORTS.length)];
            } while (destination.equals(origin));

            OffsetDateTime departure = START.plusMinutes(15L * rng.nextInt(4 * 36)); // within ~36 h
            OffsetDateTime arrival = departure.plusMinutes(30 + 15L * rng.nextInt(4 * 8)); // 30 min – 8.5 h
            Double price = rng.nextInt(10) == 0 ? null : (double) (10 + rng.nextInt(490)); // ~10% unknown

            flights.add(new Flight("R" + i, origin, destination, departure, arrival, price, "XX", String.valueOf(i)));
        }
        return flights;
    }
}
