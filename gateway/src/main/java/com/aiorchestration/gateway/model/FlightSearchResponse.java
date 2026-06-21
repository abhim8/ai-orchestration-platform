package com.aiorchestration.gateway.model;

import java.util.List;

public record FlightSearchResponse(List<Flight> flights) {
    public record Flight(String flightNumber, String origin, String destination,
                         String departureDate, String departureTime, String status) {}
}
