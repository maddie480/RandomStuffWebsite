package com.max480.randomstuff.gae.discord.customslashcommands;

import com.max480.randomstuff.gae.ConnectionUtils;
import com.max480.randomstuff.gae.SecretConstants;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * This class handles the API calls to Discord that adds or removes the guild slash commands on their end.
 */
public class CustomSlashCommandsManager {
    private static final Logger log = LoggerFactory.getLogger(CustomSlashCommandsManager.class);

    private static final String USER_AGENT = "DiscordBot (https://maddie480.ovh, 1.0)";

    private static String accessToken = null;
    private static long tokenExpiresAt = 0;

    public static class MaximumCommandsReachedException extends Exception {
    }

    /**
     * Adds a slash command with the given name and description to the given server.
     */
    public static long addSlashCommand(long serverId, String name, String description) throws IOException, MaximumCommandsReachedException {
        JSONObject commandObject = new JSONObject();
        commandObject.put("name", name);
        commandObject.put("description", description);

        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(
                "https://discord.com/api/v10/applications/" + SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_ID + "/guilds/" + serverId + "/commands");
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + authenticate());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            IOUtils.write(commandObject.toString(), os, StandardCharsets.UTF_8);
        }

        try (InputStream is = ConnectionUtils.connectionToInputStream(connection)) {
            String response = IOUtils.toString(is, StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(response);
            return Long.parseLong(o.getString("id"));
        } catch (IOException e) {
            if (connection.getResponseCode() == 400) {
                try (InputStream is = connection.getErrorStream()) {
                    String response = IOUtils.toString(is, StandardCharsets.UTF_8);
                    JSONObject o = new JSONObject(response);
                    if (o.getInt("code") == 30032) {
                        // Maximum number of application commands reached (100)
                        throw new MaximumCommandsReachedException();
                    }
                }
            }

            throw e;
        }
    }

    /**
     * Removes a slash command from a server, given its ID.
     */
    public static void removeSlashCommand(long serverId, long commandId) throws IOException {
        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(
                "https://discord.com/api/v10/applications/" + SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_ID + "/guilds/" + serverId + "/commands/" + commandId);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "Bearer " + authenticate());
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (connection.getResponseCode() != 204) {
            throw new IOException("removeSlashCommand didn't return a success code!");
        }
    }

    /**
     * Gives a slash command's description, given its ID.
     */
    public static String getSlashCommandDescription(long serverId, long commandId) throws IOException {
        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(
                "https://discord.com/api/v10/applications/" + SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_ID + "/guilds/" + serverId + "/commands/" + commandId);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + authenticate());
        connection.setRequestProperty("User-Agent", USER_AGENT);

        try (InputStream is = ConnectionUtils.connectionToInputStream(connection)) {
            String response = IOUtils.toString(is, StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(response);
            return o.getString("description");
        }
    }

    public static void warmupAuthentication() {
        try {
            authenticate();
        } catch (IOException e) {
            log.warn("Discord authentication warmup failed!", e);
        }
    }

    private static String authenticate() throws IOException {
        if (tokenExpiresAt > System.currentTimeMillis()) {
            return accessToken;
        }

        String basicAuth = Base64.getEncoder().encodeToString(
                (SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_ID + ":" + SecretConstants.CUSTOM_SLASH_COMMANDS_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout("https://discord.com/api/oauth2/token");
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            IOUtils.write("grant_type=client_credentials&scope=applications.commands.update", os, StandardCharsets.UTF_8);
        }

        try (InputStream is = ConnectionUtils.connectionToInputStream(connection)) {
            String response = IOUtils.toString(is, StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(response);

            accessToken = o.getString("access_token");
            tokenExpiresAt = System.currentTimeMillis() + (o.getInt("expires_in") * 1000L) - 5000;

            log.info("Got a new access token that expires at {}",
                    Instant.ofEpochMilli(tokenExpiresAt).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            return accessToken;
        }
    }
}
