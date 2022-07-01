package com.max480.randomstuff.gae.discord.customslashcommands;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.max480.randomstuff.gae.CloudStorageUtils;
import com.max480.randomstuff.gae.SecretConstants;
import com.max480.randomstuff.gae.discord.SodiumInstance;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static com.max480.randomstuff.gae.discord.customslashcommands.CustomSlashCommandsManager.MaximumCommandsReachedException;

/**
 * This is the API that makes Custom Slash Commands run.
 */
@WebServlet(name = "CustomSlashCommandsBot", urlPatterns = {"/discord/custom-slash-commands"})
public class InteractionManager extends HttpServlet {
    private static final Logger logger = Logger.getLogger("InteractionManager");
    private static final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // first, we want to check the Discord signature.
        byte[] body = IOUtils.toByteArray(req.getInputStream());
        String signature = req.getHeader("X-Signature-Ed25519");
        String timestamp = req.getHeader("X-Signature-Timestamp");

        byte[] timestampBytes = timestamp == null ? new byte[0] : timestamp.getBytes(StandardCharsets.UTF_8);
        byte[] signedStuff = new byte[timestampBytes.length + body.length];
        System.arraycopy(timestampBytes, 0, signedStuff, 0, timestampBytes.length);
        System.arraycopy(body, 0, signedStuff, timestampBytes.length, body.length);

        if (signature == null || timestamp == null ||
                !SodiumInstance.sodium.cryptoSignVerifyDetached(
                        hexStringToByteArray(signature),
                        signedStuff, signedStuff.length,
                        hexStringToByteArray(SecretConstants.CUSTOM_SLASH_COMMANDS_PUBLIC_KEY))) {

            // signature bad!
            logger.warning("Invalid or absent signature!");
            resp.setStatus(401);
        } else {
            // we're good! we can go on.
            // we know we're going to answer with JSON so slap the header now.
            resp.setContentType("application/json");

            JSONObject data = new JSONObject(new String(body, StandardCharsets.UTF_8));
            logger.fine("Message contents: " + data.toString(2));

            if (data.getInt("type") == 1) {
                // ping => pong
                logger.fine("Ping => Pong");
                resp.getWriter().write("{\"type\": 1}");
            } else {
                try {
                    String commandName = data.getJSONObject("data").getString("name");
                    long serverId = Long.parseLong(data.getString("guild_id"));
                    if (commandName.equals("admin")) {
                        // admin command: add or remove a slash command
                        JSONObject subcommand = data.getJSONObject("data").getJSONArray("options").getJSONObject(0);
                        if ("add".equals(subcommand.getString("name"))) {
                            addCommand(serverId, subcommand.getJSONArray("options"), resp);
                        } else {
                            removeCommand(serverId, subcommand.getJSONArray("options").getJSONObject(0).getString("value"), resp);
                        }
                    } else {
                        // another command: most definitely a custom configured one
                        handleCommand(serverId, commandName, resp);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.severe("An unexpected error occurred: " + e.toString());
                    respond(":x: An unexpected error occurred. Reach out at <https://discord.gg/59ztc8QZQ7> if this keeps happening!", resp, true);
                }
            }
        }
    }

    /**
     * Handles adding a command to a server, based on the raw options values.
     */
    private void addCommand(long serverId, JSONArray options, HttpServletResponse resp) throws IOException {
        String name = null;
        String description = null;
        String answer = null;
        boolean isPublic = false;

        for (Object item : options) {
            JSONObject itemObject = (JSONObject) item;

            switch (itemObject.getString("name")) {
                case "name":
                    name = itemObject.getString("value");
                    break;
                case "description":
                    description = itemObject.getString("value");
                    break;
                case "answer":
                    answer = itemObject.getString("value");
                    break;
                case "is_public":
                    isPublic = itemObject.getBoolean("value");
                    break;
            }
        }

        answer = answer.replace("\\n", "\n");

        if (name.equals("admin")) {
            respond(":x: \"admin\" is a reserved name!", resp, true);
        } else if (!name.matches("[a-z0-9_-]{1,32}")) {
            respond(":x: Slash command names can only contain lowercase letters, numbers, `_` or `-`, and cannot exceed 32 characters.", resp, true);
        } else if (description.length() > 100) {
            respond(":x: The description is too long! The max length is 100 characters.", resp, true);
        } else if (answer.length() > 2000) {
            respond(":x: The answer is too long! The max length is 2000 characters.", resp, true);
        } else {
            try {
                long commandId = CustomSlashCommandsManager.addSlashCommand(serverId, name, description);

                JSONObject storedData = new JSONObject();
                storedData.put("id", commandId);
                storedData.put("answer", answer);
                storedData.put("isPublic", isPublic);
                CloudStorageUtils.sendBytesToCloudStorage("custom_slash_commands/" + serverId + "/" + name + ".json",
                        "application/json", storedData.toString().getBytes(StandardCharsets.UTF_8));

                respond(":white_check_mark: The **/" + name + "** command was created.", resp, true);
            } catch (MaximumCommandsReachedException e) {
                respond(":x: **You reached the maximum amount of commands you can create on this server!**\n" +
                        "The limit set by Discord is 100 commands. Delete other commands to be able to create new ones!", resp, true);
            }
        }
    }

    /**
     * Handles removing a command from a server, based on a command name.
     */
    private void removeCommand(long serverId, String name, HttpServletResponse resp) throws IOException {
        String slashCommandPath = "custom_slash_commands/" + serverId + "/" + name + ".json";

        try (InputStream is = CloudStorageUtils.getCloudStorageInputStream(slashCommandPath)) {
            JSONObject commandInfo = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
            long commandId = commandInfo.getLong("id");

            CustomSlashCommandsManager.removeSlashCommand(serverId, commandId);

            storage.delete(BlobId.of("max480-random-stuff.appspot.com", slashCommandPath));
            respond(":white_check_mark: The **/" + name + "** command was deleted.", resp, true);
        } catch (StorageException e) {
            if (e.getCode() == 404) {
                respond(":x: The command you specified does not exist! Check that you spelled it correctly.", resp, true);
            } else {
                throw e;
            }
        }
    }

    /**
     * Handles responding to one of the custom slash commands.
     */
    private void handleCommand(long serverId, String name, HttpServletResponse resp) throws IOException {
        try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("custom_slash_commands/" + serverId + "/" + name + ".json")) {
            JSONObject commandInfo = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
            String answer = commandInfo.getString("answer");
            boolean isPublic = commandInfo.getBoolean("isPublic");
            respond(answer, resp, !isPublic);
        }
    }

    /**
     * Decodes a hex string ("28ba") to a byte array ([0x28, 0xBA]).
     *
     * @param string The string to decode
     * @return The decoded string
     */
    private byte[] hexStringToByteArray(String string) {
        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            String part = string.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(part, 16);
        }

        return bytes;
    }

    /**
     * Responds to a slash command, in response to the HTTP request.
     */
    private void respond(String message, HttpServletResponse responseStream, boolean isPrivate) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 4); // response in channel

        JSONObject responseData = new JSONObject();
        responseData.put("content", message);
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
        if (isPrivate) {
            responseData.put("flags", 1 << 6); // ephemeral
        }
        response.put("data", responseData);

        logger.fine("Responding with: " + response.toString(2));
        responseStream.getWriter().write(response.toString());
    }
}
