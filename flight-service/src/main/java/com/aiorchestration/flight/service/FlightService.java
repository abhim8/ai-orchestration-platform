package com.aiorchestration.flight.service;

import com.aiorchestration.flight.client.AmadeusClient;
import com.aiorchestration.flight.model.FlightSearchRequest;
import com.aiorchestration.flight.model.FlightSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {

    private final AmadeusClient amadeusClient;

    public FlightSearchResponse search(final FlightSearchRequest request) {
        log.debug("Delegating flight search: origin={} destination={} date={}",
                request.origin(), request.destination(), request.departureDate());

        return amadeusClient.search(request.origin(), request.destination(), request.departureDate());
    }
}
