package com.aiorchestration.flight.controller;

import com.aiorchestration.flight.model.FlightSearchRequest;
import com.aiorchestration.flight.model.FlightSearchResponse;
import com.aiorchestration.flight.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/flights")
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    public FlightSearchResponse search(@Valid final FlightSearchRequest request) {
        log.debug("Flight search request received: origin={} destination={} date={}",
                request.origin(), request.destination(), request.departureDate());

        return flightService.search(request);
    }
}
