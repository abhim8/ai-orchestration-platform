package com.aiorchestration.weather.model.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastResponse(
    Daily daily,
    Hourly hourly
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
        List<String> time,
        List<Double> temperature_2m_max,
        List<Double> temperature_2m_min,
        List<Integer> weather_code,
        List<Double> wind_speed_10m_max
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(
        List<String> time,
        List<Integer> relative_humidity_2m
    ) {}
}
