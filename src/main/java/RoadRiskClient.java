import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RoadRiskClient {
    private static final String BASE_URL = "http://localhost:8080/api";

    public static void main(String[] args) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            send(client, request("/cities").GET().build());
            String weather = """
                    {"temperature":-2,"windSpeed":5,"visibility":8000,"rain":1,"snow":0}
                    """;
            send(client, request("/risk/evaluate")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(weather))
                    .build());
            send(client, request("/risk/Kyiv").GET().build());
            send(client, request("/risk/Unknown").GET().build());
        } catch (IOException e) {
            System.err.println("Could not reach the road-risk server at " + BASE_URL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Request interrupted");
        }
    }

    private static HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(BASE_URL + path)).timeout(Duration.ofSeconds(20));
    }

    private static void send(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.printf("%s %s%nHTTP %d%n%s%n%n",
                request.method(), request.uri(), response.statusCode(), response.body());
    }
}
