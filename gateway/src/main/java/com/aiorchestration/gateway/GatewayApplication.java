package com.aiorchestration.gateway;

import com.aiorchestration.common.filter.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }
}
