package com.aiorchestration.weather.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class OpenWeatherMapClientTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    private OpenWeatherMapClient client;

    @BeforeEach
    void setUp() {
        client = new OpenWeatherMapClient(restClientBuilder);
    }

    @Test
    @DisplayName("should return mocked weather forecast")
    void shouldReturnMockedForecast() {
        var location = "Tokyo";
        var date = LocalDate.of(2026, 7, 15);

        var response = client.getForecast(location, date);

        assertNotNull(response);
        assertEquals(location, response.location());
        assertEquals(date, response.date());
    }

    @Test
    @DisplayName("should return deterministic values")
    void shouldReturnDeterministicValues() {
        var response = client.getForecast("London", LocalDate.of(2026, 8, 1));

        assertEquals(new BigDecimal("22.5"), response.temperatureCelsius());
        assertEquals("Partly cloudy", response.condition());
        assertEquals(Integer.valueOf(65), response.humidityPercent());
        assertEquals(new BigDecimal("15.3"), response.windSpeedKph());
    }

    @Test
    @DisplayName("should use provided location and date")
    void shouldUseProvidedLocationAndDate() {
        var location = "Paris";
        var date = LocalDate.of(2026, 12, 25);

        var response = client.getForecast(location, date);

        assertEquals(location, response.location());
        assertEquals(date, response.date());
    }
}
