import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Kateryna
 * SDT 213 Introduction to Software Engineering
 * assignment is due by 11:59 pm on Tuesday, November 17, 2026
 * 2-3 hours
 * a program to assess road risks. implemented by using data from weather API, assessing parameters
 * and giving user weather it is safe for a ride or not.
 * AI: was used for creating assessing of weather conditions, more specifically proposed values for check (physics concern)
 * also used to create regexes.
 */
public class Main {
    private final String API_KEY = loadApiKey();
    private final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";

    record Location(String name, double lat, double lon) {}

    // Locations to check so we do not add directly into uri. Can be improved with scanner, but it needs checks
    // for valid input, so decided to store locations in the array.
    private final Location[] LOCATIONS = {
            new Location("Kyiv", 50.4501, 30.5234),
            new Location("Lviv", 49.8397, 24.0297),
            new Location("Odesa", 46.4825, 30.7233),
            new Location("Kharkiv", 49.9935, 36.2304),
            new Location("Dnipro", 48.4647, 35.0462)
    };

    private final HttpClient client = HttpClient.newHttpClient();

    static String loadApiKey() {
        try {
            for (String line : Files.readAllLines(Path.of(".env"))) {
                if (line.startsWith("OPENWEATHER_API_KEY=")) {
                    return line.substring("OPENWEATHER_API_KEY=".length()).trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not read .env file: " + e.getMessage());
        }
        throw new IllegalStateException("OPENWEATHER_API_KEY not found in .env");
    }

    void main() {
        for (Location location : LOCATIONS) {
            try {
                checkRoadRisk(location);
            } catch (Exception e) {
                System.err.println(location.name() + " -> request failed: " + e.getMessage());
            }
            System.out.println();
        }
    }

    public void checkRoadRisk(Location location) throws Exception {
        String url = FORECAST_URL
                + "?lat=" + location.lat()
                + "&lon=" + location.lon()
                + "&units=metric&appid=" + API_KEY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + " -> " + response.body());
        }

        String body = response.body();
        // Only look at the first forecast entry the next 3 hours
        int start = body.indexOf("\"list\":[");
        int end = body.indexOf("\"dt_txt\"", start);
        String first = body.substring(start, end);

        String time = getValue(body, "dt_txt");
        double temp = toDouble(getValue(first, "temp"));
        double wind = toDouble(getValue(first, "speed"));
        double visibility = toDouble(getValue(first, "visibility"));
        double rain = getPrecipitation(first, "rain");
        double snow = getPrecipitation(first, "snow");
        String description = getValue(first, "description");

        System.out.println("    Road Risk Forecast: " + location.name() + " (" + time + " )");
        System.out.println("Weather       : " + description);
        System.out.println("Temperature   : " + temp + " °C");
        System.out.println("Wind speed    : " + wind + " m/s");
        System.out.println("Visibility    : " + visibility + " m");
        System.out.println("Rain (3h)     : " + rain + " mm");
        System.out.println("Snow (3h)     : " + snow + " mm");
        System.out.println("Road risk     : " + evaluateRisk(temp, wind, visibility, rain, snow));

    }

    // logic to estimate road danger from weather
    String evaluateRisk(double temp, double wind, double visibility, double rain, double snow) {
        if (temp <= 0 && (rain > 0 || snow > 0)) return "HIGH - ice / slippery road";
        if (snow > 0) return "HIGH - snow on road";
        if (visibility < 1000) return "HIGH - poor visibility (fog)";
        if (wind > 15) return "MEDIUM - strong wind";
        if (rain > 0) return "MEDIUM - wet road";
        if (temp <= 0) return "MEDIUM - possible black ice";
        return "LOW - normal driving conditions";
    }

    // "{\"temp\":5.51,\"humidity\":90}" example
    String getValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "N/A";         
        start = start + search.length();    

        // read until the closing quote
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        }

        int end = start;
        while (json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end);
    }

    double getPrecipitation(String json, String type) {
        int pos = json.indexOf("\"" + type + "\":{");
        if (pos == -1) return 0;
        return toDouble(getValue(json.substring(pos), "3h"));
    }

    double toDouble(String value) {
        return value.equals("N/A") ? 0 : Double.parseDouble(value);
    }
}
