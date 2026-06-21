package com.aiorchestration.flight.client;

import com.aiorchestration.flight.model.Flight;
import com.aiorchestration.flight.model.FlightSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

/**
 * Client for the Amadeus flight search API.
 *
 * <p>Currently mocked — logs the request and returns deterministic data.
 * To connect to the real API, swap the mock logic inside {@link #search}
 * for a real RestClient call while keeping the same method signature.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmadeusClient {

    private final RestClient.Builder restClientBuilder;

    /**
     * Search flights for the given origin, destination and date.
     *
     * @param origin        departure airport code
     * @param destination   arrival airport code
     * @param departureDate date of departure
     * @return mocked flight search response
     */
    public FlightSearchResponse search(final String origin,
                                       final String destination,
                                       final LocalDate departureDate) {
        log.debug("[MOCK][AMADEUS] origin={} destination={} departureDate={}",
                origin, destination, departureDate);

        var flights = List.of(
                new Flight("Air France", "AF123", origin, destination,
                        departureDate, 450.00, "USD"),
                new Flight("Lufthansa", "LH456", origin, destination,
                        departureDate, 520.00, "USD"),
                new Flight("Delta Air Lines", "DL789", origin, destination,
                        departureDate, 380.00, "USD")
        );

        return new FlightSearchResponse(flights);
    }
}
