package com.aiorchestration.gateway.config;

import com.aiorchestration.gateway.client.FlightClient;
import com.aiorchestration.gateway.client.WeatherClient;
import com.aiorchestration.gateway.service.ToolExecutor;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@link ToolExecutor} implementations to their tool names.
 * Each executor delegates to the corresponding REST client.
 */
@Slf4j
@Configuration
public class ToolExecutionConfig {

    @Bean
    Map<String, ToolExecutor> toolExecutors(final FlightClient flightClient,
                                            final WeatherClient weatherClient) {
        log.info("Registering tool executors");
        return Map.of(
                "flight.search", flightClient::searchFlights,
                "weather.forecast", weatherClient::getForecast
        );
    }
}
