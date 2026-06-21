package com.aiorchestration.flight.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FlightSearchRequest(
    @NotBlank String origin,
    @NotBlank String destination,
    @NotNull LocalDate departureDate
) {}
