# Flight Service

A microservice that provides flight search capabilities for the AI Orchestration Platform.

## Responsibilities

- Expose `GET /api/v1/flights/search` for flight searching
- Validate search parameters (origin, destination, departure date)
- Return structured flight search results

## Endpoint

```
GET /api/v1/flights/search?origin=BLR&destination=NRT&departureDate=2026-06-24
```

### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `origin` | String | IATA airport code (e.g. BLR, NRT, JFK) |
| `destination` | String | IATA airport code |
| `departureDate` | LocalDate | Departure date (ISO-8601) |

### Response

```json
{
  "flights": [
    {
      "airline": "Air France",
      "flightNumber": "AF123",
      "origin": "BLR",
      "destination": "NRT",
      "departureDate": "2026-06-24",
      "price": 850.0,
      "currency": "USD"
    }
  ]
}
```

## Data Source

The service currently uses **mocked data** - `AmadeusClient` returns three hardcoded flight results (Air France, Lufthansa, Delta) with realistic pricing. The `RestClient.Builder` bean is wired and ready for real Amadeus API integration.

## Architecture

```
FlightController → FlightService → AmadeusClient (mocked)
```

- Controller validates inputs via Jakarta Bean Validation
- Service layer for business logic
- Client layer abstracts the external API
- `GlobalExceptionHandler` maps exceptions to appropriate HTTP statuses
