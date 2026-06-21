package com.aiorchestration.weather.controller;

import com.aiorchestration.weather.model.WeatherForecastRequest;
import com.aiorchestration.weather.model.WeatherForecastResponse;
import com.aiorchestration.weather.service.WeatherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/forecast")
    public WeatherForecastResponse getForecast(@Valid final WeatherForecastRequest request) {
        log.debug("Weather forecast request received: location={} date={}",
                request.location(), request.date());

        return weatherService.getForecast(request);
    }
}
