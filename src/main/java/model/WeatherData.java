package model;

public record WeatherData(Double temperature, Double windSpeed, Double visibility,
                          Double rain, Double snow) {
    public WeatherData {
        if (temperature == null || !Double.isFinite(temperature)
                || !nonNegative(windSpeed) || !nonNegative(visibility)
                || !nonNegative(rain) || !nonNegative(snow)) {
            throw new IllegalArgumentException("Missing or invalid weather values");
        }
    }

    private static boolean nonNegative(Double value) {
        return value != null && Double.isFinite(value) && value >= 0;
    }
}

