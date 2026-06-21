package com.aiorchestration.weather.service;

import com.aiorchestration.weather.client.OpenWeatherMapClient;
import com.aiorchestration.weather.model.WeatherForecastRequest;
import com.aiorchestration.weather.model.WeatherForecastResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private OpenWeatherMapClient openWeatherMapClient;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(openWeatherMapClient);
    }

    @Test
    @DisplayName("should delegate to OpenWeatherMapClient and return response")
    void shouldDelegateToOpenWeatherMapClient() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.of(2026, 7, 15));
        var expectedResponse = new WeatherForecastResponse(
                "Tokyo", LocalDate.of(2026, 7, 15),
                new BigDecimal("22.5"), "Partly cloudy", 65, new BigDecimal("15.3"));

        when(openWeatherMapClient.getForecast("Tokyo", LocalDate.of(2026, 7, 15)))
                .thenReturn(expectedResponse);

        var response = weatherService.getForecast(request);

        assertNotNull(response);
        assertEquals("Tokyo", response.location());
        assertEquals("Partly cloudy", response.condition());
        verify(openWeatherMapClient).getForecast("Tokyo", LocalDate.of(2026, 7, 15));
    }
}
