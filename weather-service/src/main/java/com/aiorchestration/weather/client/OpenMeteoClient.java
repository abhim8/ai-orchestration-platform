package com.aiorchestration.weather.client;

import com.aiorchestration.weather.exception.LocationNotFoundException;
import com.aiorchestration.weather.model.WeatherForecastResponse;
import com.aiorchestration.weather.model.openmeteo.ForecastResponse;
import com.aiorchestration.weather.model.openmeteo.GeocodingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Client for the Open-Meteo free weather API.
 *
 * <p>Resolves location names to coordinates via
 * <a href="https://geocoding-api.open-meteo.com/v1/search">Geocoding API</a>,
 * then fetches forecast data from
 * <a href="https://api.open-meteo.com/v1/forecast">Forecast API</a>.
 *
 * <p>No authentication required. Both APIs are free with no rate limits
 * for non-commercial use.
 */
@Slf4j
@Component
public class OpenMeteoClient {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final RestClient restClient;

    public OpenMeteoClient(final RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /**
     * Fetch a weather forecast for the given location and date.
     *
     * @param location city or region name
     * @param date     forecast date
     * @return the weather forecast
     * @throws LocationNotFoundException if the location cannot be resolved
     */
    public WeatherForecastResponse getForecast(final String location, final LocalDate date) {
        log.debug("Resolving coordinates for location: {}", location);

        var geoResponse = restClient.get()
                .uri(GEOCODING_URL + "?name={name}&count=1&format=json", location)
                .retrieve()
                .body(GeocodingResponse.class);

        if (geoResponse == null || geoResponse.results() == null || geoResponse.results().isEmpty()) {
            log.warn("Location not found: {}", location);
            throw new LocationNotFoundException("Location not found: " + location);
        }

        var result = geoResponse.results().getFirst();
        var latitude = result.latitude();
        var longitude = result.longitude();

        log.debug("Resolved {} to lat={}, lon={}", location, latitude, longitude);
        log.info("Fetching forecast for {} (lat={}, lon={})", location, latitude, longitude);

        var forecastResponse = restClient.get()
                .uri(FORECAST_URL
                        + "?latitude={lat}&longitude={lon}"
                        + "&daily=temperature_2m_max,temperature_2m_min,weather_code,wind_speed_10m_max"
                        + "&hourly=relative_humidity_2m"
                        + "&timezone=auto"
                        + "&start_date={date}&end_date={date}",
                        latitude, longitude, date)
                .retrieve()
                .body(ForecastResponse.class);

        if (forecastResponse == null || forecastResponse.daily() == null) {
            log.warn("Failed to fetch forecast data for: {}", location);
            throw new RuntimeException("Failed to fetch forecast data for: " + location);
        }

        var daily = forecastResponse.daily();
        var timeIndex = findDateIndex(daily.time(), date);

        if (timeIndex < 0) {
            log.warn("Requested date {} not in forecast range, using first available", date);
            timeIndex = 0;
        }

        var maxTemp = daily.temperature_2m_max().get(timeIndex);
        var minTemp = daily.temperature_2m_min().get(timeIndex);
        var avgTemp = (maxTemp + minTemp) / 2.0;
        var weatherCode = daily.weather_code().get(timeIndex);
        var windSpeed = daily.wind_speed_10m_max().get(timeIndex);
        var condition = mapWeatherCode(weatherCode);
        var humidity = calculateMeanHumidity(forecastResponse.hourly(), date);

        return new WeatherForecastResponse(
                location,
                date,
                BigDecimal.valueOf(avgTemp).setScale(1, RoundingMode.HALF_UP),
                condition,
                humidity,
                BigDecimal.valueOf(windSpeed).setScale(1, RoundingMode.HALF_UP)
        );
    }

    /**
     * Find the index of the requested date in the daily time list.
     */
    private static int findDateIndex(final List<String> timeList, final LocalDate date) {
        var dateStr = date.toString();
        for (int i = 0; i < timeList.size(); i++) {
            if (timeList.get(i).equals(dateStr)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Calculate mean humidity from hourly data for the given date.
     */
    private static Integer calculateMeanHumidity(final ForecastResponse.Hourly hourly,
                                                  final LocalDate date) {
        if (hourly == null || hourly.time() == null || hourly.relative_humidity_2m() == null) {
            return null;
        }

        var datePrefix = date.toString();
        var sum = 0;
        var count = 0;

        for (int i = 0; i < hourly.time().size(); i++) {
            if (hourly.time().get(i).startsWith(datePrefix)) {
                sum += hourly.relative_humidity_2m().get(i);
                count++;
            }
        }

        return count > 0 ? sum / count : null;
    }

    /**
     * Map WMO weather code to a human-readable condition string.
     */
    private static String mapWeatherCode(final int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51 -> "Light drizzle";
            case 53 -> "Moderate drizzle";
            case 55 -> "Dense drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61 -> "Slight rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy rain";
            case 66, 67 -> "Freezing rain";
            case 71 -> "Slight snow";
            case 73 -> "Moderate snow";
            case 75 -> "Heavy snow";
            case 77 -> "Snow grains";
            case 80 -> "Slight rain showers";
            case 81 -> "Moderate rain showers";
            case 82 -> "Violent rain showers";
            case 85 -> "Slight snow showers";
            case 86 -> "Heavy snow showers";
            case 95 -> "Thunderstorm";
            case 96 -> "Thunderstorm with slight hail";
            case 99 -> "Thunderstorm with heavy hail";
            default -> "Unknown";
        };
    }
}
