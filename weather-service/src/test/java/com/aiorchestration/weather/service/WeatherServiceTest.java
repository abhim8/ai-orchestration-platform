package com.aiorchestration.weather.service;

import com.aiorchestration.weather.client.OpenMeteoClient;
import com.aiorchestration.weather.exception.InvalidForecastRequestException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final int MAX_DAYS_AHEAD = 16;

    @Mock
    private OpenMeteoClient openMeteoClient;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(openMeteoClient, MAX_DAYS_AHEAD);
    }

    @Test
    @DisplayName("should delegate to OpenMeteoClient and return response")
    void shouldDelegateToOpenMeteoClient() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.now().plusDays(1));
        var expectedDate = LocalDate.now().plusDays(1);
        var expectedResponse = new WeatherForecastResponse(
                "Tokyo", expectedDate,
                new BigDecimal("22.5"), "Partly cloudy", 65, new BigDecimal("15.3"));

        when(openMeteoClient.getForecast("Tokyo", expectedDate))
                .thenReturn(expectedResponse);

        var response = weatherService.getForecast(request);

        assertNotNull(response);
        assertEquals("Tokyo", response.location());
        assertEquals("Partly cloudy", response.condition());
        verify(openMeteoClient).getForecast("Tokyo", expectedDate);
    }

    @Test
    @DisplayName("today should be valid")
    void shouldAcceptToday() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.now());

        when(openMeteoClient.getForecast(eq("Tokyo"), any(LocalDate.class)))
                .thenReturn(new WeatherForecastResponse(
                        "Tokyo", LocalDate.now(), null, null, null, null));

        assertNotNull(weatherService.getForecast(request));
        verify(openMeteoClient).getForecast(eq("Tokyo"), any(LocalDate.class));
    }

    @Test
    @DisplayName("tomorrow should be valid")
    void shouldAcceptTomorrow() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.now().plusDays(1));

        when(openMeteoClient.getForecast(eq("Tokyo"), any(LocalDate.class)))
                .thenReturn(new WeatherForecastResponse(
                        "Tokyo", LocalDate.now().plusDays(1), null, null, null, null));

        assertNotNull(weatherService.getForecast(request));
        verify(openMeteoClient).getForecast(eq("Tokyo"), any(LocalDate.class));
    }

    @Test
    @DisplayName("maximum allowed day should be valid")
    void shouldAcceptMaxAllowedDay() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.now().plusDays(MAX_DAYS_AHEAD));

        when(openMeteoClient.getForecast(eq("Tokyo"), any(LocalDate.class)))
                .thenReturn(new WeatherForecastResponse(
                        "Tokyo", LocalDate.now().plusDays(MAX_DAYS_AHEAD), null, null, null, null));

        assertNotNull(weatherService.getForecast(request));
        verify(openMeteoClient).getForecast(eq("Tokyo"), any(LocalDate.class));
    }

    @Test
    @DisplayName("one day beyond maximum should throw InvalidForecastRequestException")
    void shouldRejectDateBeyondMaxDaysAhead() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.now().plusDays(MAX_DAYS_AHEAD + 1));

        var ex = assertThrows(InvalidForecastRequestException.class,
                () -> weatherService.getForecast(request));
        assertEquals("Weather forecasts are only available up to " + MAX_DAYS_AHEAD + " days in advance.",
                ex.getMessage());
        verify(openMeteoClient, never()).getForecast(anyString(), any(LocalDate.class));
    }

    @Test
    @DisplayName("past date should throw InvalidForecastRequestException")
    void shouldRejectPastDate() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.now().minusDays(1));

        var ex = assertThrows(InvalidForecastRequestException.class,
                () -> weatherService.getForecast(request));
        assertEquals("Weather forecasts are only available for today and future dates.",
                ex.getMessage());
        verify(openMeteoClient, never()).getForecast(anyString(), any(LocalDate.class));
    }
}
