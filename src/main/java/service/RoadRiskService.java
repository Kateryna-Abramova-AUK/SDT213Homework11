package service;

import model.RiskResult;
import model.WeatherData;
import org.springframework.stereotype.Service;


//road risk logic, simply based on math saying whether the road is safe
//RiskResult will hold this value
@Service
public class RoadRiskService {
    public RiskResult evaluate(String city, String time, WeatherData weather) {
        if (weather.temperature() <= 0 && (weather.rain() > 0 || weather.snow() > 0)) {
            return new RiskResult(city, time, weather, "HIGH", "Ice / slippery road");
        }
        if (weather.snow() > 0) {
            return new RiskResult(city, time, weather, "HIGH", "Snow on road");
        }
        if (weather.visibility() < 1000) {
            return new RiskResult(city, time, weather, "HIGH", "Poor visibility");
        }
        if (weather.windSpeed() > 15) {
            return new RiskResult(city, time, weather, "MEDIUM", "Strong wind");
        }
        if (weather.rain() > 0) {
            return new RiskResult(city, time, weather, "MEDIUM", "Wet road");
        }
        if (weather.temperature() <= 0) {
            return new RiskResult(city, time, weather, "MEDIUM", "Possible black ice");
        }
        return new RiskResult(city, time, weather, "LOW", "No risk threshold triggered");
    }
}
