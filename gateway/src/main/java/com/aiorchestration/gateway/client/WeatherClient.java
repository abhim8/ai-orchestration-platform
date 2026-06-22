package com.aiorchestration.gateway.client;

import com.aiorchestration.gateway.exception.DownstreamServiceException;
import com.aiorchestration.gateway.model.WeatherForecastResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for the downstream weather-service. Thin REST adapter -
 * no business logic, no AI logic, no aggregation.
 */
@Slf4j
@Component
public class WeatherClient {

    private final RestClient restClient;

    public WeatherClient(final RestClient.Builder builder,
                         @Value("${weather-service.base-url}") final String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Get weather forecast for the given location and date.
     *
     * @param arguments must contain location, date
     * @return the weather forecast response
     * @throws DownstreamServiceException if the downstream call fails
     */
    public WeatherForecastResponse getForecast(final Map<String, Object> arguments) {
        log.debug("Calling weather-service: GET /api/v1/weather/forecast?location={}&date={}",
                arguments.get("location"), arguments.get("date"));

        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/weather/forecast")
                            .queryParam("location", arguments.get("location"))
                            .queryParam("date", arguments.get("date"))
                            .build())
                    .retrieve()
                    .body(WeatherForecastResponse.class);

            log.debug("Weather forecast successful");
            return response;
        } catch (Exception e) {
            log.warn("Weather forecast failed: {} (cause: {})", e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "none");
            throw new DownstreamServiceException("Weather forecast failed: " + e.getMessage(), e);
        }
    }
}
