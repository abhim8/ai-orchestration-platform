package com.aiorchestration.gateway.config;

import com.aiorchestration.gateway.client.FlightClient;
import com.aiorchestration.gateway.client.WeatherClient;
import com.aiorchestration.gateway.service.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Wires {@link ToolExecutor} implementations to their tool names.
 * Each executor delegates to the corresponding REST client.
 */
@Slf4j
@Configuration
public class ToolExecutionConfig {

    @Bean
    RestClient.Builder restClientBuilder(
            @Value("${http.connect-timeout}") final Duration connectTimeout,
            @Value("${http.read-timeout}") final Duration readTimeout) {
        log.debug("Creating RestClient.Builder with connectTimeout={}, readTimeout={}", connectTimeout, readTimeout);
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    @Bean
    Executor toolExecutionExecutor(
            @Value("${execution.pool.core-size}") final int coreSize,
            @Value("${execution.pool.max-size}") final int maxSize,
            @Value("${execution.pool.queue-capacity}") final int queueCapacity) {
        log.info("Creating tool execution thread pool: core={}, max={}, queue={}", coreSize, maxSize, queueCapacity);
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("tool-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

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
