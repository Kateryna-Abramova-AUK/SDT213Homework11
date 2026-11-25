import model.Location;
import model.RiskResult;
import model.WeatherData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.RoadRiskService;
import service.WeatherService;

import java.util.List;

//recieves requests
@RestController
@RequestMapping("/api")
public class RoadRiskController {
    private final WeatherService weatherService;
    private final RoadRiskService riskService;

    public RoadRiskController(WeatherService weatherService, RoadRiskService riskService) {
        this.weatherService = weatherService;
        this.riskService = riskService;
    }

    @GetMapping("/cities")
    public List<Location> cities() {
        return weatherService.cities();
    }

    @GetMapping("/risk/{city}")
    public RiskResult forecast(@PathVariable String city) {
        return weatherService.forecast(city);
    }

    @GetMapping("/risk")
    public List<RiskResult> allForecasts() {
        return weatherService.allForecasts();
    }

    @PostMapping("/risk/evaluate")
    public RiskResult evaluate(@RequestBody WeatherData weather) {
        return riskService.evaluate("Manual input", null, weather);
    }
}
