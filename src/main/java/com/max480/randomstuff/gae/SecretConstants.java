package com.max480.randomstuff.gae;

import org.json.JSONObject;

/**
 * Config constants that are retrieved from environment variables on startup.
 */
public class SecretConstants {
    public static String RELOAD_SHARED_SECRET = "";

    public static String SRC_MOD_LIST_KEY = "";

    public static String GAMES_BOT_CLIENT_ID = "";
    public static String GAMES_BOT_PUBLIC_KEY = "";

    public static String CUSTOM_SLASH_COMMANDS_CLIENT_ID = "";
    public static String CUSTOM_SLASH_COMMANDS_CLIENT_SECRET = "";
    public static String CUSTOM_SLASH_COMMANDS_PUBLIC_KEY = "";

    public static String TIMEZONE_BOT_CLIENT_ID = "";
    public static String TIMEZONE_BOT_PUBLIC_KEY = "";
    public static String TIMEZONEDB_API_KEY = "";

    public static String RSS_AGGREGATOR_SECRET = "";

    static {
        // The SECRET_CONSTANTS environment variable has all secrets, in JSON format.
        String environment = System.getenv("SECRET_CONSTANTS");
        JSONObject secrets = new JSONObject(environment);

        GAMES_BOT_CLIENT_ID = secrets.getString("GAMES_BOT_CLIENT_ID");
        GAMES_BOT_PUBLIC_KEY = secrets.getString("GAMES_BOT_PUBLIC_KEY");

        CUSTOM_SLASH_COMMANDS_CLIENT_ID = secrets.getString("CUSTOM_SLASH_COMMANDS_CLIENT_ID");
        CUSTOM_SLASH_COMMANDS_CLIENT_SECRET = secrets.getString("CUSTOM_SLASH_COMMANDS_CLIENT_SECRET");
        CUSTOM_SLASH_COMMANDS_PUBLIC_KEY = secrets.getString("CUSTOM_SLASH_COMMANDS_PUBLIC_KEY");

        TIMEZONE_BOT_CLIENT_ID = secrets.getString("TIMEZONE_BOT_CLIENT_ID");
        TIMEZONE_BOT_PUBLIC_KEY = secrets.getString("TIMEZONE_BOT_PUBLIC_KEY");
        TIMEZONEDB_API_KEY = secrets.getString("TIMEZONEDB_API_KEY");

        RELOAD_SHARED_SECRET = secrets.getString("RELOAD_SHARED_SECRET");

        SRC_MOD_LIST_KEY = secrets.getString("SRC_MOD_LIST_KEY");

        RSS_AGGREGATOR_SECRET = secrets.getString("RSS_AGGREGATOR_SECRET");
    }
}
