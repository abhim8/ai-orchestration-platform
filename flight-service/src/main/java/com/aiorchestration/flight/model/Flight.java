package com.aiorchestration.flight.model;

import java.time.LocalDate;

public record Flight(
    String airline,
    String flightNumber,
    String origin,
    String destination,
    LocalDate departureDate,
    double price,
    String currency
) {}
