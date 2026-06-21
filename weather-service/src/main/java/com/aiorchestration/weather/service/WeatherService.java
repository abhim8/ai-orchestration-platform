package com.aiorchestration.weather.service;

import com.aiorchestration.weather.client.OpenWeatherMapClient;
import com.aiorchestration.weather.model.WeatherForecastRequest;
import com.aiorchestration.weather.model.WeatherForecastResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final OpenWeatherMapClient openWeatherMapClient;

    public WeatherForecastResponse getForecast(final WeatherForecastRequest request) {
        log.debug("Delegating weather forecast: location={} date={}",
                request.location(), request.date());

        return openWeatherMapClient.getForecast(request.location(), request.date());
    }
}
