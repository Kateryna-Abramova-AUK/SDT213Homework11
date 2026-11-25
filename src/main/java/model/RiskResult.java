package model;

public record RiskResult(String city, String forecastTime, WeatherData weather,
                         String riskLevel, String reason) {}
