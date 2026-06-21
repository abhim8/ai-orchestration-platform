package com.aiorchestration.flight.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class AmadeusClientTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    private AmadeusClient client;

    @BeforeEach
    void setUp() {
        client = new AmadeusClient(restClientBuilder);
    }

    @Test
    @DisplayName("should return mocked flight results")
    void shouldReturnMockedFlights() {
        var origin = "BLR";
        var destination = "NRT";
        var departureDate = LocalDate.of(2026, 7, 15);

        var response = client.search(origin, destination, departureDate);

        assertNotNull(response);
        assertNotNull(response.flights());
        assertFalse(response.flights().isEmpty());
    }

    @Test
    @DisplayName("should return three deterministic flights")
    void shouldReturnThreeFlights() {
        var origin = "BLR";
        var destination = "NRT";
        var departureDate = LocalDate.of(2026, 7, 15);

        var response = client.search(origin, destination, departureDate);

        assertEquals(3, response.flights().size());
    }

    @Test
    @DisplayName("should return flight with expected fields")
    void shouldReturnFlightWithExpectedFields() {
        var origin = "BLR";
        var destination = "NRT";
        var departureDate = LocalDate.of(2026, 7, 15);

        var response = client.search(origin, destination, departureDate);
        var flight = response.flights().getFirst();

        assertNotNull(flight.airline());
        assertNotNull(flight.flightNumber());
        assertEquals(origin, flight.origin());
        assertEquals(destination, flight.destination());
        assertEquals(departureDate, flight.departureDate());
        assertNotNull(flight.currency());
    }

    @Test
    @DisplayName("should use provided origin and destination")
    void shouldUseProvidedOriginAndDestination() {
        var origin = "JFK";
        var destination = "LHR";
        var departureDate = LocalDate.of(2026, 8, 1);

        var response = client.search(origin, destination, departureDate);

        assertEquals(origin, response.flights().getFirst().origin());
        assertEquals(destination, response.flights().getFirst().destination());
        assertEquals(departureDate, response.flights().getFirst().departureDate());
    }
}
