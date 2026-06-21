package com.aiorchestration.weather.controller;

import com.aiorchestration.weather.model.WeatherForecastRequest;
import com.aiorchestration.weather.model.WeatherForecastResponse;
import com.aiorchestration.weather.service.WeatherService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    @Mock
    private WeatherService weatherService;

    private WeatherController controller;

    private Validator validator;

    @BeforeEach
    void setUp() {
        controller = new WeatherController(weatherService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("should delegate to WeatherService and return response")
    void shouldDelegateToWeatherService() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.of(2026, 7, 15));
        var expectedResponse = new WeatherForecastResponse(
                "Tokyo", LocalDate.of(2026, 7, 15),
                new BigDecimal("22.5"), "Partly cloudy", 65, new BigDecimal("15.3"));

        when(weatherService.getForecast(any(WeatherForecastRequest.class))).thenReturn(expectedResponse);

        var response = controller.getForecast(request);

        assertNotNull(response);
        assertEquals("Tokyo", response.location());
        verify(weatherService).getForecast(request);
    }

    @Test
    @DisplayName("should reject blank location")
    void shouldRejectBlankLocation() {
        var request = new WeatherForecastRequest("", LocalDate.of(2026, 7, 15));

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location")));
    }

    @Test
    @DisplayName("should reject null date")
    void shouldRejectNullDate() {
        var request = new WeatherForecastRequest("Tokyo", null);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date")));
    }

    @Test
    @DisplayName("should accept valid request")
    void shouldAcceptValidRequest() {
        var request = new WeatherForecastRequest("Tokyo", LocalDate.of(2026, 7, 15));

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
