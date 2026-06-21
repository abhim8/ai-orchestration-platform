package com.aiorchestration.weather.client;

import com.aiorchestration.weather.exception.LocationNotFoundException;
import com.aiorchestration.weather.model.openmeteo.ForecastResponse;
import com.aiorchestration.weather.model.openmeteo.GeocodingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenMeteoClientTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OpenMeteoClient client;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        client = new OpenMeteoClient(restClientBuilder);
    }

    private void stubGeocoding(final GeocodingResponse response) {
        when(requestSpec.uri(contains("geocoding-api"), (Object) any())).thenReturn(requestSpec);
        when(responseSpec.body(GeocodingResponse.class)).thenReturn(response);
    }

    private void stubForecast(final ForecastResponse response) {
        when(requestSpec.uri(contains("api.open-meteo.com"), any(Double.class), any(Double.class), any(LocalDate.class)))
                .thenReturn(requestSpec);
        when(responseSpec.body(ForecastResponse.class)).thenReturn(response);
    }

    @Test
    @DisplayName("should return weather forecast for valid location")
    void shouldReturnForecastForValidLocation() {
        stubGeocoding(new GeocodingResponse(List.of(
                new GeocodingResponse.GeocodingResult(35.6785, 139.6823, "Tokyo"))));
        stubForecast(new ForecastResponse(
                new ForecastResponse.Daily(
                        List.of("2026-07-15"),
                        List.of(28.5),
                        List.of(22.3),
                        List.of(2),
                        List.of(15.3)),
                new ForecastResponse.Hourly(
                        List.of("2026-07-15T00:00", "2026-07-15T12:00"),
                        List.of(70, 60))));

        var response = client.getForecast("Tokyo", LocalDate.of(2026, 7, 15));

        assertNotNull(response);
        assertEquals("Tokyo", response.location());
        assertEquals(LocalDate.of(2026, 7, 15), response.date());
        assertEquals(new BigDecimal("25.4"), response.temperatureCelsius());
        assertEquals("Partly cloudy", response.condition());
        assertEquals(Integer.valueOf(65), response.humidityPercent());
        assertEquals(new BigDecimal("15.3"), response.windSpeedKph());
    }

    @Test
    @DisplayName("should throw LocationNotFoundException for null response")
    void shouldThrowForNullResponse() {
        when(requestSpec.uri(contains("geocoding-api"), (Object) any())).thenReturn(requestSpec);
        when(responseSpec.body(GeocodingResponse.class)).thenReturn(null);

        assertThrows(LocationNotFoundException.class,
                () -> client.getForecast("Nowhere", LocalDate.of(2026, 7, 15)));
    }

    @Test
    @DisplayName("should throw LocationNotFoundException when results are empty")
    void shouldThrowForEmptyResults() {
        when(requestSpec.uri(contains("geocoding-api"), (Object) any())).thenReturn(requestSpec);
        when(responseSpec.body(GeocodingResponse.class))
                .thenReturn(new GeocodingResponse(List.of()));

        assertThrows(LocationNotFoundException.class,
                () -> client.getForecast("Unknown", LocalDate.of(2026, 7, 15)));
    }

    @Test
    @DisplayName("should handle missing humidity data gracefully")
    void shouldHandleMissingHumidity() {
        stubGeocoding(new GeocodingResponse(List.of(
                new GeocodingResponse.GeocodingResult(48.8566, 2.3522, "Paris"))));
        stubForecast(new ForecastResponse(
                new ForecastResponse.Daily(
                        List.of("2026-08-01"),
                        List.of(30.0),
                        List.of(20.0),
                        List.of(0),
                        List.of(10.0)),
                null));

        var response = client.getForecast("Paris", LocalDate.of(2026, 8, 1));

        assertEquals("Paris", response.location());
        assertEquals("Clear sky", response.condition());
        assertEquals(new BigDecimal("25.0"), response.temperatureCelsius());
    }

    @Test
    @DisplayName("should use first available day when requested date is outside range")
    void shouldUseFirstDayWhenDateOutsideRange() {
        stubGeocoding(new GeocodingResponse(List.of(
                new GeocodingResponse.GeocodingResult(51.5074, -0.1278, "London"))));
        stubForecast(new ForecastResponse(
                new ForecastResponse.Daily(
                        List.of("2026-06-01"),
                        List.of(18.0),
                        List.of(12.0),
                        List.of(3),
                        List.of(20.0)),
                new ForecastResponse.Hourly(List.of(), List.of())));

        var response = client.getForecast("London", LocalDate.of(2026, 12, 25));

        assertEquals("London", response.location());
        assertEquals(new BigDecimal("15.0"), response.temperatureCelsius());
        assertEquals("Overcast", response.condition());
    }
}
