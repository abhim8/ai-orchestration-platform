package com.aiorchestration.gateway.client;

import com.aiorchestration.gateway.exception.DownstreamServiceException;
import com.aiorchestration.gateway.model.WeatherForecastResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherClientTest {

    @Mock
    private RestClient.Builder builder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private WeatherClient client;

    @BeforeEach
    void setUp() {
        when(builder.baseUrl("http://localhost:8007")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        client = new WeatherClient(builder, "http://localhost:8007");
    }

    @Test
    @DisplayName("should return weather forecast on success")
    void shouldReturnWeatherForecast() {
        var expected = new WeatherForecastResponse("Paris", "2026-06-22", 25.0, "Sunny", 70, 20.3);

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeatherForecastResponse.class)).thenReturn(expected);

        Map<String, Object> arguments = Map.of("location", "Paris", "date", "2026-06-22");
        var result = client.getForecast(arguments);

        assertNotNull(result);
        assertEquals("Paris", result.location());
        assertEquals("Sunny", result.condition());
        assertEquals(25.0, result.temperatureCelsius());
        assertEquals(70, result.humidityPercent());
        assertEquals(20.3, result.windSpeedKph());
    }

    @Test
    @DisplayName("should call the correct downstream endpoint path")
    @SuppressWarnings("unchecked")
    void shouldCallCorrectEndpoint() {
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeatherForecastResponse.class)).thenReturn(
                new WeatherForecastResponse("Paris", "2026-06-22", 25.0, "Sunny", 70, 20.3));

        Map<String, Object> arguments = Map.of("location", "Paris", "date", "2026-06-22");
        client.getForecast(arguments);

        verify(requestSpec).uri(argThat((ArgumentMatcher<Function<UriBuilder, URI>>) f -> {
            var uri = f.apply(UriComponentsBuilder.newInstance());
            return uri.toString().startsWith("/api/v1/weather/forecast");
        }));
    }

    @Test
    @DisplayName("should throw DownstreamServiceException on network error")
    void shouldThrowOnNetworkError() {
        when(restClient.get()).thenThrow(new RuntimeException("Connection refused"));

        Map<String, Object> arguments = Map.of("location", "Paris", "date", "2026-06-22");

        var ex = assertThrows(DownstreamServiceException.class,
                () -> client.getForecast(arguments));
        assertTrue(ex.getMessage().contains("Weather forecast failed"));
    }

    @Test
    @DisplayName("should throw DownstreamServiceException on non-2xx response")
    void shouldThrowOnNon2xx() {
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenThrow(new RuntimeException("500 Internal Server Error"));

        Map<String, Object> arguments = Map.of("location", "Paris", "date", "2026-06-22");

        var ex = assertThrows(DownstreamServiceException.class,
                () -> client.getForecast(arguments));
        assertTrue(ex.getMessage().contains("Weather forecast failed"));
    }

    @Test
    @DisplayName("should return null when downstream returns null")
    void shouldReturnNullOnNullResponse() {
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeatherForecastResponse.class)).thenReturn(null);

        Map<String, Object> arguments = Map.of("location", "Paris", "date", "2026-06-22");
        var result = client.getForecast(arguments);

        assertNull(result);
    }

    @Test
    @DisplayName("should deserialize from weather-service JSON contract")
    void shouldDeserializeFromJson() throws Exception {
        var json = """
                {
                  "location": "Bangalore",
                  "date": "2026-06-21",
                  "temperatureCelsius": 30.5,
                  "condition": "Overcast",
                  "humidityPercent": 70,
                  "windSpeedKph": 20.3
                }
                """;

        var mapper = new ObjectMapper();
        var result = mapper.readValue(json, WeatherForecastResponse.class);

        assertNotNull(result);
        assertEquals("Bangalore", result.location());
        assertEquals("2026-06-21", result.date());
        assertEquals(30.5, result.temperatureCelsius());
        assertEquals("Overcast", result.condition());
        assertEquals(70, result.humidityPercent());
        assertEquals(20.3, result.windSpeedKph());
    }
}
