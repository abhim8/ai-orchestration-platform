package com.aiorchestration.gateway.client;

import com.aiorchestration.gateway.exception.DownstreamServiceException;
import com.aiorchestration.gateway.model.FlightSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightClientTest {

    @Mock
    private RestClient.Builder builder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private FlightClient client;

    @BeforeEach
    void setUp() {
        when(builder.baseUrl("http://localhost:8081")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        client = new FlightClient(builder, "http://localhost:8081");
    }

    @Test
    @DisplayName("should return flight search response on success")
    void shouldReturnFlightSearchResponse() {
        var expected = new FlightSearchResponse(List.of(
                new FlightSearchResponse.Flight("AA123", "LHR", "CDG", "2026-06-22", "10:00", "scheduled")
        ));

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(FlightSearchResponse.class)).thenReturn(expected);

        Map<String, Object> arguments = Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22");
        var result = client.searchFlights(arguments);

        assertNotNull(result);
        assertEquals(1, result.flights().size());
        assertEquals("AA123", result.flights().getFirst().flightNumber());
    }

    @Test
    @DisplayName("should throw DownstreamServiceException on network error")
    void shouldThrowOnNetworkError() {
        when(restClient.get()).thenThrow(new RuntimeException("Connection refused"));

        Map<String, Object> arguments = Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22");

        var ex = assertThrows(DownstreamServiceException.class,
                () -> client.searchFlights(arguments));
        assertTrue(ex.getMessage().contains("Flight search failed"));
    }

    @Test
    @DisplayName("should throw DownstreamServiceException on non-2xx response")
    void shouldThrowOnNon2xx() {
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenThrow(new RuntimeException("500 Internal Server Error"));

        Map<String, Object> arguments = Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22");

        var ex = assertThrows(DownstreamServiceException.class,
                () -> client.searchFlights(arguments));
        assertTrue(ex.getMessage().contains("Flight search failed"));
    }

    @Test
    @DisplayName("should return null when downstream returns null")
    void shouldReturnNullOnNullResponse() {
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(Function.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(FlightSearchResponse.class)).thenReturn(null);

        Map<String, Object> arguments = Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22");
        var result = client.searchFlights(arguments);

        assertNull(result);
    }
}
