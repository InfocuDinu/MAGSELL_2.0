package com.bakerymanager.smartbill.invoicing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
public class SmartBillApiClient {

    private static final Logger logger = LoggerFactory.getLogger(SmartBillApiClient.class);

    private final SmartBillApiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SmartBillApiClient(SmartBillApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .build();
    }

    public SmartBillApiCallResult sendNirDocument(Map<String, Object> payload) {
        if (!properties.isConfigured()) {
            throw new SmartBillApiException(
                "SmartBill API nu este configurat (email/token/CUI/baseUrl/path).",
                false,
                0
            );
        }

        int maxAttempts = Math.max(1, properties.getRetryMaxAttempts());
        long delayMs = Math.max(1, properties.getRetryInitialDelayMs());

        SmartBillApiException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = doSend(payload);
                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    String documentId = extractDocumentId(response.body());
                    String message = extractMessage(response.body(), "Export NIR realizat cu succes.");
                    return new SmartBillApiCallResult(documentId, message, attempt, statusCode);
                }

                boolean retryable = isRetryableStatus(statusCode);
                String message = extractMessage(response.body(), "Eroare SmartBill API");
                lastException = new SmartBillApiException(message, retryable, statusCode);

                if (!retryable || attempt == maxAttempts) {
                    throw lastException;
                }

                sleepBeforeRetry(delayMs, attempt, maxAttempts, statusCode);
                delayMs = delayMs * 2;
            } catch (IOException ex) {
                lastException = new SmartBillApiException(
                    "Eroare IO la comunicarea cu SmartBill: " + ex.getMessage(),
                    true,
                    0,
                    ex
                );

                if (attempt == maxAttempts) {
                    throw lastException;
                }

                sleepBeforeRetry(delayMs, attempt, maxAttempts, 0);
                delayMs = delayMs * 2;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new SmartBillApiException("Execuția a fost întreruptă în timpul apelului SmartBill.", false, 0, ex);
            }
        }

        throw lastException != null
            ? lastException
            : new SmartBillApiException("Export SmartBill a eșuat fără detalii.", false, 0);
    }

    private HttpResponse<String> doSend(Map<String, Object> payload) throws IOException, InterruptedException {
        String url = normalizeUrl(properties.getBaseUrl(), properties.getStockDocumentPath());
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(properties.getTimeoutMs()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Basic " + basicAuthValue(properties.getEmail(), properties.getToken()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String basicAuthValue(String username, String password) {
        String raw = username + ":" + password;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeUrl(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void sleepBeforeRetry(long delayMs, int attempt, int maxAttempts, int statusCode) {
        logger.warn("SmartBill call failed (attempt {}/{}), status {}. Retrying in {} ms.",
            attempt, maxAttempts, statusCode, delayMs);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SmartBillApiException("Execuția a fost întreruptă în timpul retry-ului SmartBill.", false, statusCode, e);
        }
    }

    private String extractDocumentId(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("id")) {
                return root.get("id").asText();
            }
            if (root.hasNonNull("documentId")) {
                return root.get("documentId").asText();
            }
            if (root.has("data") && root.get("data").hasNonNull("id")) {
                return root.get("data").get("id").asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractMessage(String body, String fallback) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
            if (root.hasNonNull("errorText")) {
                return root.get("errorText").asText();
            }
            if (root.hasNonNull("errorMessage")) {
                return root.get("errorMessage").asText();
            }
            if (root.has("error") && root.get("error").isTextual()) {
                return root.get("error").asText();
            }
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public record SmartBillApiCallResult(String documentId, String message, int attempts, int statusCode) {
    }
}
