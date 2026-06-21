package com.aiorchestration.flight.controller;

import com.aiorchestration.flight.model.FlightSearchRequest;
import com.aiorchestration.flight.model.FlightSearchResponse;
import com.aiorchestration.flight.service.FlightService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    @Mock
    private FlightService flightService;

    private FlightController controller;

    private Validator validator;

    @BeforeEach
    void setUp() {
        controller = new FlightController(flightService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("should delegate to FlightService and return response")
    void shouldDelegateToFlightService() {
        var request = new FlightSearchRequest("BLR", "NRT", LocalDate.of(2026, 7, 15));
        var expectedResponse = new FlightSearchResponse(List.of());

        when(flightService.search(any(FlightSearchRequest.class))).thenReturn(expectedResponse);

        var response = controller.search(request);

        assertNotNull(response);
        verify(flightService).search(request);
    }

    @Test
    @DisplayName("should reject blank origin")
    void shouldRejectBlankOrigin() {
        var request = new FlightSearchRequest("", "NRT", LocalDate.of(2026, 7, 15));

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("origin")));
    }

    @Test
    @DisplayName("should reject blank destination")
    void shouldRejectBlankDestination() {
        var request = new FlightSearchRequest("BLR", "", LocalDate.of(2026, 7, 15));

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("destination")));
    }

    @Test
    @DisplayName("should reject null departureDate")
    void shouldRejectNullDepartureDate() {
        var request = new FlightSearchRequest("BLR", "NRT", null);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("departureDate")));
    }

    @Test
    @DisplayName("should accept valid request")
    void shouldAcceptValidRequest() {
        var request = new FlightSearchRequest("BLR", "NRT", LocalDate.of(2026, 7, 15));

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
