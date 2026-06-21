package com.aiorchestration.flight.model;

import java.util.List;

public record FlightSearchResponse(
    List<Flight> flights
) {}
