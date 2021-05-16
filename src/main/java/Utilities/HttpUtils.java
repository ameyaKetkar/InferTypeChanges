package Utilities;

import io.vavr.Tuple;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class HttpUtils {

    public static Optional<String> makeHttpRequest(Map<String, String> header) {
        try {
            String[] objects = header.entrySet().stream().map(Tuple::fromEntry).flatMap(x -> x.toSeq().toJavaStream())
                    .toArray(String[]::new);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/"))
                    .headers(objects)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .build().send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.ofNullable(response.body());
        } catch (InterruptedException | URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
