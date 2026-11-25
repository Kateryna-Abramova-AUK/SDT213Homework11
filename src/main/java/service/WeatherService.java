package service;

import model.Location;
import model.RiskResult;
import model.WeatherData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

//forecast logic when we provide a city
//reads the temperature, wind speed, visibility, rain, and snow
// into a WeatherData object
@Service
public class WeatherService {
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private static final List<Location> CITIES = List.of(
            new Location("Kyiv", 50.4501, 30.5234),
            new Location("Lviv", 49.8397, 24.0297),
            new Location("Odesa", 46.4825, 30.7233),
            new Location("Kharkiv", 49.9935, 36.2304),
            new Location("Dnipro", 48.4647, 35.0462)
    );

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final JsonMapper mapper;
    private final RoadRiskService riskService;
    private final String apiKey;

    public WeatherService(JsonMapper mapper, RoadRiskService riskService,
                          @Value("${OPENWEATHER_API_KEY:}") String apiKey) {
        this.mapper = mapper;
        this.riskService = riskService;
        this.apiKey = apiKey.trim();
    }

    public List<Location> cities() {
        return CITIES;
    }

    public RiskResult forecast(String city) {
        Location location = CITIES.stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(city))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unsupported city"));
        return forecast(location);
    }

    public List<RiskResult> allForecasts() {
        return CITIES.stream().map(this::forecast).toList();
    }

    private RiskResult forecast(Location location) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Weather API key not configured");
        }

        String url = FORECAST_URL + "?lat=" + location.lat() + "&lon=" + location.lon()
                + "&units=metric&cnt=1&appid=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather provider rejected the request");
            }
            return parseForecast(location, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Weather request interrupted");
        } catch (IOException | JacksonException | IllegalArgumentException e) {
            // Provider errors can include request details. Keep them out of the API response.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather provider unavailable or invalid forecast");
        }
    }

    private RiskResult parseForecast(Location location, String body) {
        JsonNode first = mapper.readTree(body).path("list").path(0);
        String time = first.path("dt_txt").stringValue("");
        if (time.isBlank()) {
            throw new IllegalArgumentException("Missing forecast time");
        }

        WeatherData weather = new WeatherData(first.path("main").path("temp").doubleValue(Double.NaN),
                first.path("wind").path("speed").doubleValue(Double.NaN),
                first.path("visibility").doubleValue(Double.NaN),
                precipitation(first, "rain"), precipitation(first, "snow"));
        return riskService.evaluate(location.name(), time + " UTC", weather);
    }

    private double precipitation(JsonNode forecast, String type) {
        return forecast.has(type) ? forecast.path(type).path("3h").doubleValue(Double.NaN) : 0;
    }
}
