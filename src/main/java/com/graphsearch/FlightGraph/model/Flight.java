package com.graphsearch.FlightGraph.model;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Flight {
    private String id;
    private String origin;
    private String destination;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;
    private Double price;          // null when there is no price for that day
    private String carrier;        // e.g. "LH"
    private String flightNumber;   // e.g. "1139"
}