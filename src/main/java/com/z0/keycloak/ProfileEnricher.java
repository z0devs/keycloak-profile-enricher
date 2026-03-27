package com.z0.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class ProfileEnricher {

    private static final Logger log = Logger.getLogger(ProfileEnricher.class);

    private static final String SERVICE_ENDPOINT =
            System.getenv().getOrDefault("ENRICHER_SERVICE_ENDPOINT", "https://your-internal-service.com");

    private static final String SERVICE_SECRET =
            System.getenv().getOrDefault("ENRICHER_SERVICE_SECRET", "your-secret");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper JSON = new ObjectMapper();

    private ProfileEnricher() {}

    @SuppressWarnings("unchecked")
    private static Map<String, String> fetchAttributes(UserModel user) throws Exception {
        String body = JSON.writeValueAsString(Map.of(
                "email",      nullSafe(user.getEmail()),
                "firstName",  nullSafe(user.getFirstName()),
                "lastName",   nullSafe(user.getLastName()),
                "keycloakId", user.getId()
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVICE_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("X-Internal-Secret", SERVICE_SECRET)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response = HTTP.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Enricher service returned HTTP "
                    + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> parsed = JSON.readValue(response.body(), Map.class);
        Map<String, String> attributes = new HashMap<>();

        parsed.forEach((key, value) -> {
            if (value == null) return;
            if (value instanceof Map || value instanceof java.util.List) {
                log.warnf("Skipping nested attribute '%s' for user %s — only flat values supported",
                        key, user.getId());
                return;
            }
            attributes.put(key, value.toString());
        });

        log.infof("Fetched %d attributes for user %s: %s",
                attributes.size(), user.getId(), attributes.keySet());

        return attributes;
    }

    public static void enrich(UserModel user) {
        String sentinel = user.getFirstAttribute(ProfileEnricherConfig.ENRICHED_SENTINEL);
        if ("true".equals(sentinel)) {
            log.debugf("User %s already enriched, skipping", user.getId());
            return;
        }

        try {
            Map<String, String> attributes = fetchAttributes(user);

            if (attributes.isEmpty()) {
                log.warnf("Service returned empty response for user %s", user.getId());
                return;
            }

            attributes.forEach((key, value) -> {
                user.setSingleAttribute(key, value);
                log.debugf("  attribute set: %s = %s", key, value);
            });

            user.setSingleAttribute(ProfileEnricherConfig.ENRICHED_SENTINEL, "true");

        } catch (Exception e) {
            log.errorf(e, "Enrichment failed for user %s — will retry on next login", user.getId());
        }
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}