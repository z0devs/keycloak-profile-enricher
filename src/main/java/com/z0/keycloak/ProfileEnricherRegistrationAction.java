package com.z0.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

public class ProfileEnricherRegistrationAction implements FormAction, FormActionFactory {

    private static final Logger log = Logger.getLogger(ProfileEnricherRegistrationAction.class);

    @Override
    public void success(FormContext context) {
        try {
            ProfileEnricher.enrich(context.getUser());
        } catch (Exception e) {
            log.errorf(e, "Enrichment failed during registration for user %s",
                    context.getUser().getId());
        }
    }

    @Override public void buildPage(FormContext ctx, LoginFormsProvider form) {}
    @Override public void validate(ValidationContext ctx) { ctx.success(); }
    @Override public boolean requiresUser() { return false; }
    @Override public boolean configuredFor(KeycloakSession s, RealmModel r, UserModel u) { return true; }
    @Override public void setRequiredActions(KeycloakSession s, RealmModel r, UserModel u) {}

    @Override public String getDisplayType() { return "Profile Enricher — Populate on Register"; }
    @Override public String getReferenceCategory() { return "profile-enricher"; }
    @Override public boolean isConfigurable() { return false; }
    @Override public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }
    @Override public boolean isUserSetupAllowed() { return false; }
    @Override public String getHelpText() { return "Calls external service to populate user attributes on self-registration."; }
    @Override public List<ProviderConfigProperty> getConfigProperties() { return Collections.emptyList(); }
    @Override public FormAction create(KeycloakSession session) { return this; }
    @Override public void init(Config.Scope config) {}
    @Override public void postInit(KeycloakSessionFactory factory) {}
    @Override public void close() {}
    @Override public String getId() { return ProfileEnricherConfig.REGISTRATION_ACTION_ID; }
}