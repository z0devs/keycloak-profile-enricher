package com.z0.keycloak;

public final class ProfileEnricherConfig {
    private ProfileEnricherConfig() {}

    // Sentinel attribute — written after successful enrichment
    // Prevents duplicate service calls on subsequent logins
    public static final String ENRICHED_SENTINEL = "_enriched";

    public static final String REGISTRATION_ACTION_ID = "profile-enricher-registration";
    public static final String FIRST_LOGIN_AUTH_ID    = "profile-enricher-first-login";
    public static final String EVENT_LISTENER_ID      = "profile-enricher-event-listener";
}