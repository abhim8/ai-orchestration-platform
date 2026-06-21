package com.aiorchestration.gateway.client;

import com.aiorchestration.gateway.exception.DownstreamServiceException;
import com.aiorchestration.gateway.model.FlightSearchResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for the downstream flight-service. Thin REST adapter —
 * no business logic, no AI logic, no aggregation.
 */
@Slf4j
@Component
public class FlightClient {

    private final RestClient restClient;

    public FlightClient(final RestClient.Builder builder,
                        @Value("${flight-service.base-url}") final String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Search flights with the given arguments.
     *
     * @param arguments must contain origin, destination, departureDate
     * @return the flight search response
     * @throws DownstreamServiceException if the downstream call fails
     */
    public FlightSearchResponse searchFlights(final Map<String, Object> arguments) {
        log.debug("Searching flights: origin={}, destination={}, date={}",
                arguments.get("origin"), arguments.get("destination"), arguments.get("departureDate"));

        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/flights/search")
                            .queryParam("origin", arguments.get("origin"))
                            .queryParam("destination", arguments.get("destination"))
                            .queryParam("departureDate", arguments.get("departureDate"))
                            .build())
                    .retrieve()
                    .body(FlightSearchResponse.class);

            log.debug("Flight search successful");
            return response;
        } catch (Exception e) {
            log.warn("Flight search failed: {}", e.getMessage());
            throw new DownstreamServiceException("Flight search failed: " + e.getMessage(), e);
        }
    }
}
