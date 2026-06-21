package com.aiorchestration.weather.service;

import com.aiorchestration.weather.client.OpenMeteoClient;
import com.aiorchestration.weather.exception.InvalidForecastRequestException;
import com.aiorchestration.weather.model.WeatherForecastRequest;
import com.aiorchestration.weather.model.WeatherForecastResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;
    private final int maxDaysAhead;

    public WeatherService(final OpenMeteoClient openMeteoClient,
                          @Value("${weather.forecast.max-days-ahead}") final int maxDaysAhead) {
        this.openMeteoClient = openMeteoClient;
        this.maxDaysAhead = maxDaysAhead;
    }

    public WeatherForecastResponse getForecast(final WeatherForecastRequest request) {
        log.debug("Delegating weather forecast: location={} date={}",
                request.location(), request.date());

        validateForecastDate(request.date());

        return openMeteoClient.getForecast(request.location(), request.date());
    }

    private void validateForecastDate(final LocalDate date) {
        var today = LocalDate.now();

        if (date.isBefore(today)) {
            log.warn("Rejected weather request because requested date {} is in the past", date);
            throw new InvalidForecastRequestException(
                    "Weather forecasts are only available for today and future dates.");
        }

        var maxAllowed = today.plusDays(maxDaysAhead);
        if (date.isAfter(maxAllowed)) {
            log.warn("Rejected weather request because requested date {} exceeds supported forecast window. "
                            + "Maximum allowed date is {} ({} days ahead)",
                    date, maxAllowed, maxDaysAhead);
            throw new InvalidForecastRequestException(
                    "Weather forecasts are only available up to " + maxDaysAhead + " days in advance.");
        }
    }
}
