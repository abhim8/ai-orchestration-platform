package com.aiorchestration.weather.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeatherForecastResponse(
    String location,
    LocalDate date,
    BigDecimal temperatureCelsius,
    String condition,
    Integer humidityPercent,
    BigDecimal windSpeedKph
) {}
