package org.example.genpass.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** main для Docker HEALTHCHECK: GET /api/health → exit 0/1. */
public final class HealthCheck {

    private HealthCheck() {
    }

    public static void main(String[] args) {
        int port = defaultPort();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host() + ":" + port + "/api/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.exit(response.statusCode() == 200 ? 0 : 1);
        } catch (Exception e) {
            System.exit(1);
        }
    }

    /** HOST из env (как у сервера); wildcard-адрес 0.0.0.0 для запроса заменяем на loopback. */
    private static String host() {
        String host = System.getenv().getOrDefault("HOST", "0.0.0.0");
        return host.isBlank() || "0.0.0.0".equals(host) ? "127.0.0.1" : host;
    }

    private static int defaultPort() {
        String value = System.getenv("PORT");
        if (value == null || value.isBlank()) {
            return 8080;
        }
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                return 8080;
            }
            return port;
        } catch (NumberFormatException e) {
            return 8080;
        }
    }
}
