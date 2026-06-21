package com.aiorchestration.weather.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WeatherForecastRequest(
    @NotBlank String location,
    @NotNull LocalDate date
) {}
