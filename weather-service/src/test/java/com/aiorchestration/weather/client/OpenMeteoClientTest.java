package com.aiorchestration.weather.client;

import com.aiorchestration.weather.exception.LocationNotFoundException;
import com.aiorchestration.weather.model.openmeteo.ForecastResponse;
import com.aiorchestration.weather.model.openmeteo.GeocodingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @Captor
    private ArgumentCaptor<URI> uriCaptor;

    private OpenMeteoClient client;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(URI.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        client = new OpenMeteoClient(restClientBuilder);
        ReflectionTestUtils.setField(client, "geocodingUrl", "https://geocoding-api.open-meteo.com/v1/search");
        ReflectionTestUtils.setField(client, "forecastUrl", "https://api.open-meteo.com/v1/forecast");
    }

    private void stubGeocoding(final GeocodingResponse response) {
        when(responseSpec.body(GeocodingResponse.class)).thenReturn(response);
    }

    private void stubForecast(final ForecastResponse response) {
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
        when(responseSpec.body(GeocodingResponse.class)).thenReturn(null);

        assertThrows(LocationNotFoundException.class,
                () -> client.getForecast("Nowhere", LocalDate.of(2026, 7, 15)));
    }

    @Test
    @DisplayName("should throw LocationNotFoundException when results are empty")
    void shouldThrowForEmptyResults() {
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

    @Test
    @DisplayName("should construct valid HTTPS URIs without double-slash bug")
    void shouldConstructValidHttpsUris() {
        stubGeocoding(new GeocodingResponse(List.of(
                new GeocodingResponse.GeocodingResult(35.6785, 139.6823, "Tokyo"))));
        stubForecast(new ForecastResponse(
                new ForecastResponse.Daily(
                        List.of("2026-07-15"),
                        List.of(28.5),
                        List.of(22.3),
                        List.of(2),
                        List.of(15.3)),
                new ForecastResponse.Hourly(List.of(), List.of())));

        client.getForecast("Tokyo", LocalDate.of(2026, 7, 15));

        verify(requestSpec, org.mockito.Mockito.times(2)).uri(uriCaptor.capture());
        var uris = uriCaptor.getAllValues();

        assertEquals(2, uris.size());

        assertTrue(uris.get(0).toString().startsWith("https://geocoding-api.open-meteo.com/v1/search"),
                "Geocoding URI should start with https:// (double slash preserved), but was: " + uris.get(0));
        assertTrue(uris.get(0).toString().contains("name=Tokyo"),
                "Geocoding URI should contain location query param");

        assertTrue(uris.get(1).toString().startsWith("https://api.open-meteo.com/v1/forecast"),
                "Forecast URI should start with https:// (double slash preserved), but was: " + uris.get(1));
        assertTrue(uris.get(1).toString().contains("latitude=35.6785"),
                "Forecast URI should contain latitude query param");
        assertTrue(uris.get(1).toString().contains("longitude=139.6823"),
                "Forecast URI should contain longitude query param");
    }
}
