# Keycloak Profile Enricher

Dynamically fetches and populates custom user attributes from an external service across all user creation paths. Supports Keycloak 24+.

> **Ownership & Credits:** Created and maintained by **Zone Zero**.

## Overview

When a new user is created in Keycloak, this extension pauses the flow, sends a webhook containing the user's base details to your external API, and maps the returned JSON data into the user's Keycloak profile as custom attributes. 

To ensure complete coverage across the Keycloak ecosystem, this plugin utilizes three separate Service Provider Interfaces (SPIs) to catch users regardless of how they are created:
1. **Self-Registration (UI):** Catches users signing up via the standard Keycloak login/register form.
2. **Social / SSO Login:** Catches users logging in for the first time via Identity Providers (Google, GitHub, SAML, etc.).
3. **Admin Creation:** Catches users created manually via the Keycloak Admin Console or the Admin REST API.

**Note on Deduplication:** The extension automatically injects an `_enriched=true` sentinel attribute into the user's profile upon successful enrichment. If multiple events fire simultaneously (e.g., a First Login event and a base Register event), the plugin will instantly skip the duplicate request.

---

## ⚙️ Environment Variables

The extension is configured entirely via environment variables. Set these in your Keycloak container/deployment:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `ENRICHER_SERVICE_ENDPOINT` | `https://your-internal-service.com` | The full, absolute URL of your external API endpoint that will process the webhook. |
| `ENRICHER_SERVICE_SECRET` | `your-secret` | A static secret injected into the `X-Internal-Secret` header of the outgoing request so your API can verify the caller. |

---

## 🔌 The API Contract

Your external service must be prepared to accept a `POST` request and return a **flat JSON object**.

### 1. The Outgoing Request (From Keycloak to your API)
The plugin will send a POST request with the `Content-Type: application/json` and the `X-Internal-Secret` header. 

**Payload:**
```json
{
  "email": "user@domain.com",
  "firstName": "John",
  "lastName": "Doe",
  "keycloakId": "a13d8210-5232-436a-8178-520f30727576"
}
```

### 2. The Expected Response (From your API to Keycloak)
Your API must return an HTTP `200 OK` with a flat JSON object representing the attributes you want to inject.

**Expected Payload:**
```json
{
  "tenant_id": "workspace_99",
  "company_name": "Zone Zero",
  "custom_role_flag": "true"
}
```
*⚠️ **Important:** Nested JSON objects and Arrays are not supported by Keycloak attributes. If your API returns nested structures, the plugin will log a warning and skip those specific keys.*

---

## 🚀 Installation & Setup

1. Build your `.jar` file using Maven/Gradle.
2. Place the `.jar` file into your Keycloak `providers/` directory (e.g., `/opt/keycloak/providers/`).
3. Rebuild Keycloak (`kc.sh build`) if running in optimized production mode, then start the server.

### Enabling the Triggers (Keycloak Admin Console)

Because the plugin uses three different listeners, you must explicitly enable the ones you want to use in your Realm settings.

#### A. Catch Admin/REST API Creations (Event Listener)
1. Go to **Realm settings** -> **Events** -> **Config**.
2. Under **Event Listeners**, click the input box and select `profile-enricher-event-listener`.
3. Click **Save**.

#### B. Catch Standard UI Registrations (Form Action)
1. Go to **Authentication**.
2. Select the **Registration** flow. *(If it is Built-in, click the 3 dots, select "Duplicate", and bind the duplicated flow to "Registration").*
3. At the bottom of the execution steps, click **Add execution**.
4. Select **Profile Enricher — Populate on Register**.
5. Set its requirement to **REQUIRED**.

#### C. Catch Social/SSO Logins (First Broker Login)
1. Go to **Authentication**.
2. Select the **First Broker Login** flow. *(Duplicate if Built-in).*
3. Click **Add execution**.
4. Select **Profile Enricher — First Login Populator**.
5. Set its requirement to **REQUIRED**.

---

## 📜 Logs and Debugging

The extension uses Keycloak's standard JBoss logging.
* On success, you will see an `INFO` log: `Fetched X attributes for user [UUID]: [keys]`
* If the API fails or times out, you will see an `ERROR` log: `Enrichment failed for user [UUID] — will retry on next login`.
* You can enable `DEBUG` logging in your Keycloak configuration for `com.z0.keycloak` to see exactly which individual attributes are being written in real-time.

---
*Copyright © Zone Zero. All rights reserved.*
```