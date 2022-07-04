package com.max480.randomstuff.gae.discord.customslashcommands;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.goterl.lazysodium.exceptions.SodiumException;
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
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.max480.randomstuff.gae.discord.customslashcommands.CustomSlashCommandsManager.MaximumCommandsReachedException;

/**
 * This is the API that makes Custom Slash Commands run.
 */
@WebServlet(name = "CustomSlashCommandsBot", urlPatterns = {"/discord/custom-slash-commands"}, loadOnStartup = 5)
public class InteractionManager extends HttpServlet {
    private static final Logger logger = Logger.getLogger("InteractionManager");
    private static final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

    @Override
    public void init() {
        try {
            logger.info("Computing sha512 with sodium for warmup: " + SodiumInstance.sodium.cryptoHashSha512("warmup"));
        } catch (SodiumException e) {
            logger.log(Level.WARNING, "Sodium warmup failed! " + e.toString());
        }
    }

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
            } else if (data.getInt("type") == 5) {
                // edit form submit
                String[] customIdCut = data.getJSONObject("data").getString("custom_id").split("\\|");
                long serverId = Long.parseLong(customIdCut[0]);
                String commandName = customIdCut[1];

                editCommand(serverId, commandName, data.getJSONObject("data").getJSONArray("components"), resp);
            } else {
                // slash command invocation
                try {
                    String commandName = data.getJSONObject("data").getString("name");
                    long serverId = Long.parseLong(data.getString("guild_id"));
                    if (commandName.equals("addc")) {
                        // admin command: add a command to the server
                        addCommand(serverId, data.getJSONObject("data").getJSONArray("options"), resp);
                    } else if (commandName.equals("removec")) {
                        // admin command: remove a command from the server
                        removeCommand(serverId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), resp);
                    } else if (commandName.equals("editc")) {
                        // admin command: show the modal allowing to edit a command
                        sendEditCommandForm(serverId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), resp);
                    } else if (commandName.equals("clist")) {
                        // admin command: show the modal allowing to edit a command
                        listCommands(serverId, resp);
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

        if (name.equals("addc") || name.equals("removec") || name.equals("editc") || name.equals("clist")) {
            respond(":x: You cannot name a command `addc`, `removec`, `editc` or `clist`! This would conflict with the built-in commands.", resp, true);
        } else if (!name.matches("[a-z0-9_-]{1,32}")) {
            respond(":x: Slash command names can only contain lowercase letters, numbers, `_` or `-`, and cannot exceed 32 characters.", resp, true);
        } else if (description.length() > 100) {
            respond(":x: The description is too long! The max length is 100 characters.", resp, true);
        } else if (answer.length() > 2000) {
            respond(":x: The answer is too long! The max length is 2000 characters.", resp, true);
        } else {
            try {
                addSlashCommand(serverId, name, description, answer, isPublic);

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
            removeSlashCommand(serverId, slashCommandPath, is);
            respond(":white_check_mark: The **/" + name + "** command was deleted.", resp, true);
        } catch (StorageException e) {
            if (e.getCode() == 404) {
                respond(":x: The command you specified does not exist! Check that you spelled it correctly.", resp, true);
            } else {
                throw e;
            }
        }
    }

    private void listCommands(long serverId, HttpServletResponse resp) throws IOException {
        String prefix = "custom_slash_commands/" + serverId + "/";

        Set<String> commands = new TreeSet<>();
        Page<Blob> blobs = storage.list("max480-random-stuff.appspot.com", Storage.BlobListOption.prefix(prefix));
        for (Blob blob : blobs.iterateAll()) {
            String commandName = blob.getName();
            commandName = commandName.substring(prefix.length(), commandName.lastIndexOf("."));
            commands.add(commandName);
        }

        if (commands.isEmpty()) {
            respond("There is no command on this server yet.", resp, true);
        } else {
            String message = "Here is the list of commands registered on this server:\n/" + String.join("\n/", commands);
            if (message.length() > 2000) {
                message = message.substring(0, 1995) + "[...]";
            }
            respond(message, resp, true);
        }
    }

    /**
     * Allows to display the "edit slash command" form.
     */
    private void sendEditCommandForm(long serverId, String name, HttpServletResponse resp) throws IOException {
        String slashCommandPath = "custom_slash_commands/" + serverId + "/" + name + ".json";

        try (InputStream is = CloudStorageUtils.getCloudStorageInputStream(slashCommandPath)) {
            // get info on the current command
            JSONObject commandInfo = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
            long commandId = commandInfo.getLong("id");
            String description = CustomSlashCommandsManager.getSlashCommandDescription(serverId, commandId);
            String answer = commandInfo.getString("answer");
            boolean isPublic = commandInfo.getBoolean("isPublic");

            // build the response
            JSONObject response = new JSONObject();
            response.put("type", 9); // modal

            JSONObject responseData = new JSONObject();
            responseData.put("custom_id", serverId + "|" + name);
            responseData.put("title", "Edit – /" + name);

            JSONArray componentData = new JSONArray();
            componentData.put(getComponentDataForTextInput("name", "Name", name, 32, false));
            componentData.put(getComponentDataForTextInput("description", "Description", description, 100, false));
            componentData.put(getComponentDataForTextInput("answer", "Answer", answer, 2000, true));
            componentData.put(getComponentDataForTextInput("isPublic", "Response is public (true or false)", isPublic ? "true" : "false", 5, false));
            responseData.put("components", componentData);

            response.put("data", responseData);
            logger.fine("Responding with: " + response.toString(2));
            resp.getWriter().write(response.toString());
        } catch (StorageException e) {
            if (e.getCode() == 404) {
                respond(":x: The command you specified does not exist! Check that you spelled it correctly.", resp, true);
            } else {
                throw e;
            }
        }
    }

    /**
     * Gives the JSON object describing a single text input.
     */
    private static JSONObject getComponentDataForTextInput(String id, String name, String value, long maxLength, boolean isLong) {
        JSONObject row = new JSONObject();
        row.put("type", 1); // ... action row

        JSONObject component = new JSONObject();
        component.put("type", 4); // text input
        component.put("custom_id", id);
        component.put("label", name);
        component.put("style", isLong ? 2 : 1);
        component.put("min_length", 1);
        component.put("max_length", maxLength);
        component.put("required", true);
        component.put("value", value);

        JSONArray rowContent = new JSONArray();
        rowContent.put(component);
        row.put("components", rowContent);

        return row;
    }

    /**
     * Handles actually editing a slash command.
     */
    private void editCommand(long serverId, String oldName, JSONArray options, HttpServletResponse resp) throws IOException {
        String newName = null;
        String description = null;
        String answer = null;
        String isPublic = null;

        for (Object item : options) {
            JSONObject itemObject = ((JSONObject) item).getJSONArray("components").getJSONObject(0);

            switch (itemObject.getString("custom_id")) {
                case "name":
                    newName = itemObject.getString("value");
                    break;
                case "description":
                    description = itemObject.getString("value");
                    break;
                case "answer":
                    answer = itemObject.getString("value");
                    break;
                case "isPublic":
                    isPublic = itemObject.getString("value");
                    break;
            }
        }

        if (newName.equals("addc") || newName.equals("removec") || newName.equals("editc") || newName.equals("clist")) {
            respond(":x: You cannot name a command `addc`, `removec`, `editc` or `clist`! This would conflict with the built-in commands.", resp, true);
        } else if (!newName.matches("[a-z0-9_-]{1,32}")) {
            respond(":x: Slash command names can only contain lowercase letters, numbers, `_` or `-`, and cannot exceed 32 characters.", resp, true);
        } else if (!Arrays.asList("true", "false").contains(isPublic)) {
            respond(":x: The value for \"Response is public\" should be either \"true\" or \"false\"!", resp, true);
        } else {
            String slashCommandPath = "custom_slash_commands/" + serverId + "/" + oldName + ".json";
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream(slashCommandPath)) {
                // since adding a command is an upsert, deleting first is only required if the name changed.
                if (!oldName.equals(newName)) {
                    removeSlashCommand(serverId, slashCommandPath, is);
                }

                addSlashCommand(serverId, newName, description, answer, Boolean.parseBoolean(isPublic));

                if (oldName.equals(newName)) {
                    respond(":white_check_mark: The **/" + newName + "** command was edited.", resp, true);
                } else {
                    respond(":white_check_mark: The **/" + oldName + "** command was edited, and is now called **/" + newName + "**.", resp, true);
                }
            } catch (StorageException e) {
                if (e.getCode() == 404) {
                    respond(":x: The command you tried to edit does not exist anymore!", resp, true);
                } else {
                    throw e;
                }
            } catch (MaximumCommandsReachedException e) {
                // this error is not expected here.
                throw new IOException(e);
            }
        }
    }

    /**
     * Handles adding a slash command on Cloud Storage and Discord given all the information about it.
     */
    private void addSlashCommand(long serverId, String name, String description, String answer, boolean isPublic) throws IOException, MaximumCommandsReachedException {
        String path = "custom_slash_commands/" + serverId + "/" + name + ".json";
        logger.info("Adding " + path);

        long commandId = CustomSlashCommandsManager.addSlashCommand(serverId, name, description);

        JSONObject storedData = new JSONObject();
        storedData.put("id", commandId);
        storedData.put("answer", answer);
        storedData.put("isPublic", isPublic);
        CloudStorageUtils.sendBytesToCloudStorage(path, "application/json", storedData.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Handles removing a slash command from Cloud Storage and Discord given all the information about it.
     */
    private void removeSlashCommand(long serverId, String slashCommandPath, InputStream cloudStorageInputStream) throws IOException {
        logger.info("Removing " + slashCommandPath);

        JSONObject commandInfo = new JSONObject(IOUtils.toString(cloudStorageInputStream, StandardCharsets.UTF_8));
        long commandId = commandInfo.getLong("id");

        CustomSlashCommandsManager.removeSlashCommand(serverId, commandId);

        storage.delete(BlobId.of("max480-random-stuff.appspot.com", slashCommandPath));
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
