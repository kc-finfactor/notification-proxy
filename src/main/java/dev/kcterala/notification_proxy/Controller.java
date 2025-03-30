package dev.kcterala.notification_proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
public class Controller {
    private static final String FINSENSE = "finsense";
    private static final int TIMEOUT_SECONDS = 5;
    private static final String CONSENT_API_PATH = "/Consent/Notification";
    private static final String DATA_API_PATH = "/FI/Notification";

    private final ProxyRepository proxyRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Controller(final ProxyRepository proxyRepository, final ObjectMapper objectMapper) {
        this.proxyRepository = proxyRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    @PostMapping("/Consent/Notification")
    public ResponseEntity<Object> ingestNotification(@RequestBody final Object payload) {
        final String team = FINSENSE;
        final List<Proxy> proxies = proxyRepository.findByTeam(team);
        if (proxies == null || proxies.isEmpty()) {
            return ResponseEntity.internalServerError().body("Sorry no one's available to take this request");
        }

        try {
            return forwardNotification(proxies, payload, CONSENT_API_PATH);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process notification: " + e.getMessage());
        }
    }

    public ResponseEntity<Object> forwardNotification(final List<Proxy> proxies, final Object payload, final String path) {
        try {
            final String jsonPayload = objectMapper.writeValueAsString(payload);
            final List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();


            for (final Proxy proxy : proxies) {
                final HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(proxy.getProxyUrl() + "/finsense/API/V2" + path))
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .build();

                final CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(
                        request, HttpResponse.BodyHandlers.ofString());

                futures.add(future);
            }


            final CompletableFuture<?>[] futuresArray = futures.toArray(new CompletableFuture[0]);
            final Object result = CompletableFuture.anyOf(futuresArray).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            final HttpResponse<String> firstResponse = (HttpResponse<String>) result;

            // If this first response is successful, return it
            if (firstResponse.statusCode() >= 200 && firstResponse.statusCode() < 300) {
                final Object responseBody = objectMapper.readValue(firstResponse.body(), Object.class);
                return ResponseEntity.status(firstResponse.statusCode()).body(responseBody);
            }

            // If not, check if any other completed future was successful
            for (final CompletableFuture<HttpResponse<String>> future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    try {
                        final HttpResponse<String> response = future.get();
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            Object responseBody = objectMapper.readValue(response.body(), Object.class);
                            return ResponseEntity.status(response.statusCode()).body(responseBody);
                        }
                    } catch (Exception ignored) {
                    }
                }


            }

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("No successful responses received from any proxy");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Failed to process notification: " + e.getMessage());
        }
    }
}
