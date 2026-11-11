package com.graphsearch.FlightGraph.routing;

import com.graphsearch.FlightGraph.model.Flight;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-only reference implementation. It is intentionally independent from the production code
 * (it doesn't use FlightNetwork, ConnectionRules.allows or the Itinerary getters), so tests can
 * compare your algorithms against it without trusting them.
 *
 * Brute force: tries every path with a DFS. Exponential, but fine for tiny networks.
 */
final class ReferenceRoutes {

    private ReferenceRoutes() {}

    /** Every valid path for the query, as lists of flights. */
    static List<List<Flight>> allPaths(Collection<Flight> flights, RouteQuery query, ConnectionRules rules) {
        List<List<Flight>> result = new ArrayList<>();
        for (Flight first : flights) {
            if (first.getOrigin().equals(query.origin())
                    && first.getDepartureTime().toLocalDate().equals(query.date())) {
                List<Flight> path = new ArrayList<>(List.of(first));
                Set<String> visited = new HashSet<>(Set.of(first.getOrigin(), first.getDestination()));
                dfs(flights, query, rules, path, visited, result);
            }
        }
        return result;
    }

    private static void dfs(Collection<Flight> flights, RouteQuery query, ConnectionRules rules,
                            List<Flight> path, Set<String> visited, List<List<Flight>> result) {
        Flight last = path.get(path.size() - 1);
        if (last.getDestination().equals(query.destination())) {
            result.add(List.copyOf(path));
            return; // a journey ends when it reaches the destination
        }
        if (path.size() - 1 == query.maxStops()) {
            return;
        }
        for (Flight next : flights) {
            if (next.getOrigin().equals(last.getDestination())
                    && !visited.contains(next.getDestination())
                    && canConnect(last, next, rules)) {
                path.add(next);
                visited.add(next.getDestination());
                dfs(flights, query, rules, path, visited, result);
                visited.remove(next.getDestination());
                path.remove(path.size() - 1);
            }
        }
    }

    static boolean canConnect(Flight arriving, Flight departing, ConnectionRules rules) {
        Duration layover = Duration.between(arriving.getArrivalTime().toInstant(), departing.getDepartureTime().toInstant());
        return layover.compareTo(rules.minConnectionTime()) >= 0 && layover.compareTo(rules.maxLayover()) <= 0;
    }

    static Instant arrival(List<Flight> path) {
        return path.get(path.size() - 1).getArrivalTime().toInstant();
    }

    /** Total price, or null if any leg has no price. */
    static Double price(List<Flight> path) {
        if (path.stream().map(Flight::getPrice).anyMatch(Objects::isNull)) {
            return null;
        }
        return path.stream().mapToDouble(Flight::getPrice).sum();
    }

    static Optional<Instant> earliestArrival(Collection<Flight> flights, RouteQuery query, ConnectionRules rules) {
        return allPaths(flights, query, rules).stream().map(ReferenceRoutes::arrival).min(Instant::compareTo);
    }

    static Optional<Double> lowestPrice(Collection<Flight> flights, RouteQuery query, ConnectionRules rules) {
        return allPaths(flights, query, rules).stream().map(ReferenceRoutes::price)
                .filter(Objects::nonNull).min(Double::compareTo);
    }

    /** Fails the test if the itinerary breaks any rule of the query — checked from scratch. */
    static void assertValid(Itinerary itinerary, RouteQuery query, ConnectionRules rules) {
        List<Flight> legs = itinerary.legs();
        assertThat(legs).as("legs").isNotEmpty();
        assertThat(legs.get(0).getOrigin()).as("first leg origin").isEqualTo(query.origin());
        assertThat(legs.get(legs.size() - 1).getDestination()).as("last leg destination").isEqualTo(query.destination());
        assertThat(legs.get(0).getDepartureTime().toLocalDate()).as("first leg local departure date")
                .isEqualTo(query.date());
        assertThat(legs.size() - 1).as("stops").isLessThanOrEqualTo(query.maxStops());

        Set<String> visited = new HashSet<>(Set.of(legs.get(0).getOrigin()));
        for (int i = 0; i < legs.size(); i++) {
            Flight leg = legs.get(i);
            assertThat(visited.add(leg.getDestination())).as("airport visited twice: " + leg.getDestination()).isTrue();
            if (i > 0) {
                Flight previous = legs.get(i - 1);
                assertThat(leg.getOrigin()).as("leg %d chained", i).isEqualTo(previous.getDestination());
                assertThat(canConnect(previous, leg, rules)).as("connection %s -> %s respects rules",
                        previous.getId(), leg.getId()).isTrue();
            }
        }
    }
}
