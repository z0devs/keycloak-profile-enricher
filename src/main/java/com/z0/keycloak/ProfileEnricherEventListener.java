package com.z0.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;

public class ProfileEnricherEventListener implements EventListenerProvider, EventListenerProviderFactory {

    private static final Logger log = Logger.getLogger(ProfileEnricherEventListener.class);
    private KeycloakSession session;

    public ProfileEnricherEventListener() {}
    private ProfileEnricherEventListener(KeycloakSession session) { this.session = session; }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.REGISTER) {
            handle(event.getRealmId(), event.getUserId());
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event.getResourceType() == ResourceType.USER
                && event.getOperationType() == OperationType.CREATE) {
            String userId = extractUserId(event.getResourcePath());
            if (userId != null) handle(event.getRealmId(), userId);
        }
    }

    private void handle(String realmId, String userId) {
        if (userId == null || userId.isBlank()) return;
        try {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) return;
            UserModel user = session.users().getUserById(realm, userId);
            if (user != null) ProfileEnricher.enrich(user);
        } catch (Exception e) {
            log.errorf(e, "Enrichment failed for user %s", userId);
        }
    }

    private String extractUserId(String path) {
        if (path == null) return null;
        String[] parts = path.split("/");
        return parts.length >= 2 ? parts[parts.length - 1] : null;
    }

    @Override public void close() {}
    @Override public EventListenerProvider create(KeycloakSession session) { return new ProfileEnricherEventListener(session); }
    @Override public void init(Config.Scope config) {}
    @Override public void postInit(KeycloakSessionFactory factory) {}
    @Override public String getId() { return ProfileEnricherConfig.EVENT_LISTENER_ID; }
}