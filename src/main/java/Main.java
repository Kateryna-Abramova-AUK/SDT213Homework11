/**
 * Kateryna
 * SDT 213 Introduction to Software Engineering
 * assignment is due by 11:59 pm on Tuesday, November 24, 2026
 * 2-3 hours
 * Assesses road risks using OpenWeather forecasts or manually supplied weather data.
 * AI assistance: used for debugging and writing client
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import service.RoadRiskService;
import service.WeatherService;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({RoadRiskController.class, RoadRiskService.class, WeatherService.class})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
