package com.aiorchestration.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherForecastResponse(
    String location,
    String date,
    double temperatureCelsius,
    String condition,
    int humidityPercent,
    double windSpeedKph
) {}
