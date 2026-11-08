package com.graphsearch.FlightGraph.model;

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
public class Airport {
    private String code;
    private String name;
    private String city;
    private String country;
    private double latitude;
    private double longitude;
    private String timeZoneId; // e.g. "Europe/Lisbon" — needed to convert local times into real instants
}
