package com.graphsearch.FlightGraph.model;

import java.time.OffsetDateTime;

public class Flight {
    private String id;
    private String origin;
    private String destination;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;
    private double price;
    private String carrier;

    public Flight() {}

    public Flight(String id, String origin, String destination,
                  OffsetDateTime departureTime, OffsetDateTime arrivalTime,
                  double price, String carrier) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.carrier = carrier;
    }

    // getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public OffsetDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(OffsetDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public OffsetDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(OffsetDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    @Override
    public String toString() {
        return "Flight [id=" + id + ", origin=" + origin + ", destination=" + destination +  ", departureTime=" + departureTime + ", arrivalTime=" + arrivalTime + ", price=" + price + ", carrier=" + carrier + "]";
    }
}