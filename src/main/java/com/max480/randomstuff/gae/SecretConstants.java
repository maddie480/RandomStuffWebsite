package com.max480.randomstuff.gae;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Config constants that are retrieved from Google Cloud Secret Manager on startup.
 */
public class SecretConstants {
    private static final Logger logger = Logger.getLogger("SecretConstants");

    public static String RELOAD_SHARED_SECRET = "";

    public static String SRC_MOD_LIST_KEY = "";

    public static String GAMES_BOT_CLIENT_ID = "";
    public static String GAMES_BOT_PUBLIC_KEY = "";

    public static String CUSTOM_SLASH_COMMANDS_CLIENT_ID = "";
    public static String CUSTOM_SLASH_COMMANDS_CLIENT_SECRET = "";
    public static String CUSTOM_SLASH_COMMANDS_PUBLIC_KEY = "";

    public static String MATTERMOST_TOKEN_LOCK = "";
    public static String MATTERMOST_TOKEN_UNLOCK = "";
    public static String MATTERMOST_TOKEN_EXPLOIT = "";
    public static String MATTERMOST_TOKEN_ABSENTS = "";
    public static String MATTERMOST_TOKEN_CONSISTENCYCHECK = "";

    static {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            // FRONTEND_SECRETS contains all the secrets, in JSON format.
            SecretVersionName secretVersionName = SecretVersionName.of("max480-random-stuff", "FRONTEND_SECRETS", "1");
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
            JSONObject secrets = new JSONObject(response.getPayload().getData().toStringUtf8());

            GAMES_BOT_CLIENT_ID = secrets.getString("GAMES_BOT_CLIENT_ID");
            GAMES_BOT_PUBLIC_KEY = secrets.getString("GAMES_BOT_PUBLIC_KEY");

            CUSTOM_SLASH_COMMANDS_CLIENT_ID = secrets.getString("CUSTOM_SLASH_COMMANDS_CLIENT_ID");
            CUSTOM_SLASH_COMMANDS_CLIENT_SECRET = secrets.getString("CUSTOM_SLASH_COMMANDS_CLIENT_SECRET");
            CUSTOM_SLASH_COMMANDS_PUBLIC_KEY = secrets.getString("CUSTOM_SLASH_COMMANDS_PUBLIC_KEY");

            MATTERMOST_TOKEN_ABSENTS = secrets.getString("MATTERMOST_TOKEN_ABSENTS");
            MATTERMOST_TOKEN_CONSISTENCYCHECK = secrets.getString("MATTERMOST_TOKEN_CONSISTENCYCHECK");
            MATTERMOST_TOKEN_EXPLOIT = secrets.getString("MATTERMOST_TOKEN_EXPLOIT");
            MATTERMOST_TOKEN_LOCK = secrets.getString("MATTERMOST_TOKEN_LOCK");
            MATTERMOST_TOKEN_UNLOCK = secrets.getString("MATTERMOST_TOKEN_UNLOCK");

            RELOAD_SHARED_SECRET = secrets.getString("RELOAD_SHARED_SECRET");

            SRC_MOD_LIST_KEY = secrets.getString("SRC_MOD_LIST_KEY");
        } catch (IOException e) {
            logger.severe("Could not load application secrets! " + e.toString());
        }
    }
}
