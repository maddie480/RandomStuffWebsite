package ovh.maddie480.randomstuff.frontend;

import org.json.JSONObject;

/**
 * Config constants that are retrieved from environment variables on startup.
 */
public class SecretConstants {
    public static final String RELOAD_SHARED_SECRET;

    public static final String SRC_MOD_LIST_KEY;

    public static final String GAMES_BOT_CLIENT_ID;
    public static final String GAMES_BOT_PUBLIC_KEY;

    public static final String CUSTOM_SLASH_COMMANDS_CLIENT_ID;
    public static final String CUSTOM_SLASH_COMMANDS_CLIENT_SECRET;
    public static final String CUSTOM_SLASH_COMMANDS_PUBLIC_KEY;

    public static final String TIMEZONE_BOT_CLIENT_ID;
    public static final String TIMEZONE_BOT_PUBLIC_KEY;
    public static final String TIMEZONEDB_API_KEY;

    public static final String BANANABOT_PUBLIC_KEY;

    public static final String OLYMPUS_NEWS_MANAGER_CLIENT_ID;
    public static final String OLYMPUS_NEWS_MANAGER_PUBLIC_KEY;

    public static final String GITHUB_SSH_KNOWN_HOSTS;
    public static final String GITHUB_SSH_PUBLIC_KEY;
    public static final String GITHUB_SSH_PRIVATE_KEY;

    public static final String UNHANDLED_EXCEPTIONS_WEBHOOK_URL;

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

        BANANABOT_PUBLIC_KEY = secrets.getString("BANANABOT_PUBLIC_KEY");

        OLYMPUS_NEWS_MANAGER_CLIENT_ID = secrets.getString("OLYMPUS_NEWS_MANAGER_CLIENT_ID");
        OLYMPUS_NEWS_MANAGER_PUBLIC_KEY = secrets.getString("OLYMPUS_NEWS_MANAGER_PUBLIC_KEY");

        RELOAD_SHARED_SECRET = secrets.getString("RELOAD_SHARED_SECRET");

        SRC_MOD_LIST_KEY = secrets.getString("SRC_MOD_LIST_KEY");

        GITHUB_SSH_KNOWN_HOSTS = secrets.getString("GITHUB_SSH_KNOWN_HOSTS");
        GITHUB_SSH_PUBLIC_KEY = secrets.getString("GITHUB_SSH_PUBLIC_KEY");
        GITHUB_SSH_PRIVATE_KEY = secrets.getString("GITHUB_SSH_PRIVATE_KEY");

        UNHANDLED_EXCEPTIONS_WEBHOOK_URL = secrets.getString("UNHANDLED_EXCEPTIONS_WEBHOOK_URL");
    }
}
