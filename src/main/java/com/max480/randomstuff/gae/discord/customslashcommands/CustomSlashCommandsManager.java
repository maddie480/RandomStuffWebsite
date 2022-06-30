package com.max480.randomstuff.gae.discord.customslashcommands;

import com.max480.randomstuff.gae.SecretConstants;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * This class handles the API calls to Discord that adds or removes the guild slash commands on their end.
 */
public class CustomSlashCommandsManager {
    private static final String USER_AGENT = "DiscordBot (https://max480-random-stuff.appspot.com, 1.0)";

    /**
     * Adds a slash command with the given name and description to the given server.
     */
    public static long addSlashCommand(long serverId, String name, String description) throws IOException {
        JSONObject commandObject = new JSONObject();
        commandObject.put("name", name);
        commandObject.put("description", description);

        HttpURLConnection connection = (HttpURLConnection) new URL("https://discord.com/api/v10/applications/" + SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_ID + "/guilds/" + serverId + "/commands").openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + authenticate());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            IOUtils.write(commandObject.toString(), os, StandardCharsets.UTF_8);
        }

        try (InputStream is = connection.getInputStream()) {
            String response = IOUtils.toString(is, StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(response);
            return Long.parseLong(o.getString("id"));
        }
    }

    /**
     * Removes a slash command from a server, given its ID.
     */
    public static void removeSlashCommand(long serverId, long commandId) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://discord.com/api/v10/applications/" + SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_ID + "/guilds/" + serverId + "/commands/" + commandId).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "Bearer " + authenticate());
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (connection.getResponseCode() != 204) {
            throw new IOException("removeSlashCommand didn't return a success code!");
        }
    }

    private static String authenticate() throws IOException {
        String basicAuth = Base64.getEncoder().encodeToString(
                (SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_ID + ":" + SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

        HttpURLConnection connection = (HttpURLConnection) new URL("https://discord.com/api/oauth2/token").openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            IOUtils.write("grant_type=client_credentials&scope=applications.commands.update", os, StandardCharsets.UTF_8);
        }

        try (InputStream is = connection.getInputStream()) {
            String response = IOUtils.toString(is, StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(response);
            return o.getString("access_token");
        }
    }
}
