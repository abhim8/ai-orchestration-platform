package com.aiorchestration.weather.client;

import com.aiorchestration.weather.model.WeatherForecastResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Client for the OpenWeatherMap API.
 *
 * <p>Currently mocked — logs the request and returns deterministic data.
 * To connect to the real API, replace the body of {@link #getForecast}
 * with a real RestClient call while keeping the same method signature.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenWeatherMapClient {

    private final RestClient.Builder restClientBuilder;

    /**
     * Fetch a weather forecast for the given location and date.
     *
     * @param location city or region name
     * @param date     forecast date
     * @return mocked weather forecast response
     */
    public WeatherForecastResponse getForecast(final String location,
                                               final LocalDate date) {
        log.info("[MOCK][OPENWEATHER] location={} date={}", location, date);

        return new WeatherForecastResponse(
                location,
                date,
                new BigDecimal("22.5"),
                "Partly cloudy",
                65,
                new BigDecimal("15.3")
        );
    }
}
