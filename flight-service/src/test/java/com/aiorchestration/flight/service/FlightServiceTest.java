package com.aiorchestration.flight.service;

import com.aiorchestration.flight.client.AmadeusClient;
import com.aiorchestration.flight.model.Flight;
import com.aiorchestration.flight.model.FlightSearchRequest;
import com.aiorchestration.flight.model.FlightSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private AmadeusClient amadeusClient;

    private FlightService flightService;

    @BeforeEach
    void setUp() {
        flightService = new FlightService(amadeusClient);
    }

    @Test
    @DisplayName("should delegate to AmadeusClient and return response")
    void shouldDelegateToAmadeusClient() {
        var request = new FlightSearchRequest("BLR", "NRT", LocalDate.of(2026, 7, 15));
        var expectedResponse = new FlightSearchResponse(List.of(
                new Flight("Test Air", "TA001", "BLR", "NRT",
                        LocalDate.of(2026, 7, 15), 300.00, "USD")
        ));

        when(amadeusClient.search("BLR", "NRT", LocalDate.of(2026, 7, 15)))
                .thenReturn(expectedResponse);

        var response = flightService.search(request);

        assertNotNull(response);
        assertEquals(1, response.flights().size());
        assertEquals("Test Air", response.flights().getFirst().airline());
        verify(amadeusClient).search("BLR", "NRT", LocalDate.of(2026, 7, 15));
    }
}
