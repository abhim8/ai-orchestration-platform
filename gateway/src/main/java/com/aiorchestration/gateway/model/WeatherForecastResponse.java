package com.aiorchestration.gateway.model;

public record WeatherForecastResponse(String location, String date,
                                      String condition, double temperature) {}
