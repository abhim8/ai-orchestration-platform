package com.aiorchestration.weather.model.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResponse(
    List<GeocodingResult> results
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingResult(
        double latitude,
        double longitude,
        String name
    ) {}
}
