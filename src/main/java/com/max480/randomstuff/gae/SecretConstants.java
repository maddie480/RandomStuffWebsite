package com.max480.randomstuff.gae;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Config constants that are retrieved from Google Cloud Secret Manager on startup.
 */
public class SecretConstants {
    private static final Logger logger = Logger.getLogger("SecretConstants");

    public static String CATALOG_RELOAD_SHARED_SECRET = "";
    public static String SERVICE_MONITORING_SECRET = "";

    public static String SRC_MOD_LIST_KEY = "";

    public static String GITHUB_USERNAME = "";
    public static String GITHUB_PERSONAL_ACCESS_TOKEN = "";

    public static String LOGGING_EXPECTED_AUTH_HEADER = "";

    public static String GAMES_BOT_CLIENT_ID = "";
    public static String GAMES_BOT_PUBLIC_KEY = "";

    public static String EXPLOIT_PLANNING_URL = "";
    public static String MATTERMOST_TOKEN_VACANCES = "";
    public static String MATTERMOST_TOKEN_LOCK = "";
    public static String MATTERMOST_TOKEN_UNLOCK = "";
    public static String MATTERMOST_TOKEN_EXPLOIT = "";
    public static String MATTERMOST_TOKEN_ABSENTS = "";
    public static String MATTERMOST_TOKEN_CONSISTENCYCHECK = "";
    public static String YOUTUBE_API_KEY = "";

    static {
        Map<String, Consumer<String>> secrets = new HashMap<>();
        secrets.put("CATALOG_RELOAD_SHARED_SECRET", s -> CATALOG_RELOAD_SHARED_SECRET = s);
        secrets.put("SERVICE_MONITORING_SECRET", s -> SERVICE_MONITORING_SECRET = s);
        secrets.put("SRC_MOD_LIST_KEY", s -> SRC_MOD_LIST_KEY = s);
        secrets.put("GITHUB_USERNAME", s -> GITHUB_USERNAME = s);
        secrets.put("GITHUB_PERSONAL_ACCESS_TOKEN", s -> GITHUB_PERSONAL_ACCESS_TOKEN = s);
        secrets.put("LOGGING_EXPECTED_AUTH_HEADER", s -> LOGGING_EXPECTED_AUTH_HEADER = s);
        secrets.put("GAMES_BOT_CLIENT_ID", s -> GAMES_BOT_CLIENT_ID = s);
        secrets.put("GAMES_BOT_PUBLIC_KEY", s -> GAMES_BOT_PUBLIC_KEY = s);
        secrets.put("EXPLOIT_PLANNING_URL", s -> EXPLOIT_PLANNING_URL = s);
        secrets.put("MATTERMOST_TOKEN_VACANCES", s -> MATTERMOST_TOKEN_VACANCES = s);
        secrets.put("MATTERMOST_TOKEN_LOCK", s -> MATTERMOST_TOKEN_LOCK = s);
        secrets.put("MATTERMOST_TOKEN_UNLOCK", s -> MATTERMOST_TOKEN_UNLOCK = s);
        secrets.put("MATTERMOST_TOKEN_EXPLOIT", s -> MATTERMOST_TOKEN_EXPLOIT = s);
        secrets.put("MATTERMOST_TOKEN_ABSENTS", s -> MATTERMOST_TOKEN_ABSENTS = s);
        secrets.put("MATTERMOST_TOKEN_CONSISTENCYCHECK", s -> MATTERMOST_TOKEN_CONSISTENCYCHECK = s);
        secrets.put("YOUTUBE_API_KEY", s -> YOUTUBE_API_KEY = s);

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            for (Map.Entry<String, Consumer<String>> secret : secrets.entrySet()) {
                SecretVersionName secretVersionName = SecretVersionName.of("max480-random-stuff", secret.getKey(), "1");
                AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
                String payload = response.getPayload().getData().toStringUtf8();
                secret.getValue().accept(payload);
            }
        } catch (IOException e) {
            logger.severe("Could not load application secrets! " + e.toString());
        }

    }
}
