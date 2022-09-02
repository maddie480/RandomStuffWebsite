package com.max480.randomstuff.gae.discord.customslashcommands;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.max480.randomstuff.gae.CloudStorageUtils;
import com.max480.randomstuff.gae.SecretConstants;
import com.max480.randomstuff.gae.discord.DiscordProtocolHandler;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
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
        DiscordProtocolHandler.warmup();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject data = DiscordProtocolHandler.validateRequest(req, resp, SecretConstants.CUSTOM_SLASH_COMMANDS_PUBLIC_KEY);
        if (data == null) return;

        if (data.getInt("type") == 5) {
            // edit form submit
            String[] customIdCut = data.getJSONObject("data").getString("custom_id").split("\\|");
            long serverId = Long.parseLong(customIdCut[0]);
            String commandName = customIdCut[1];

            editCommand(serverId, commandName, data.getJSONObject("data").getJSONArray("components"), data.getString("locale"), resp);
        } else {
            String locale = data.getString("locale");

            // slash command invocation
            try {
                String commandName = data.getJSONObject("data").getString("name");
                long serverId = Long.parseLong(data.getString("guild_id"));

                switch (commandName) {
                    case "addc":
                        // admin command: add a command to the server
                        addCommand(serverId, data.getJSONObject("data").getJSONArray("options"), locale, resp);
                        break;

                    case "removec":
                        // admin command: remove a command from the server
                        removeCommand(serverId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale, resp);
                        break;

                    case "editc":
                        // admin command: show the modal allowing to edit a command
                        sendEditCommandForm(serverId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale, resp);
                        break;

                    case "clist":
                        // admin command: show the modal allowing to edit a command
                        listCommands(serverId, locale, resp);
                        break;

                    default:
                        // another command: most definitely a custom configured one
                        handleCommand(serverId, commandName, resp);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.severe("An unexpected error occurred: " + e);
                respond(resp, true, localizeMessage(locale,
                        ":x: An unexpected error occurred. Reach out at <https://discord.gg/59ztc8QZQ7> if this keeps happening!",
                        ":x: Une erreur inattendue est survenue. Signale-le sur <https://discord.gg/59ztc8QZQ7> si ça continue à arriver !"));
            }
        }
    }

    /**
     * Represents slash command info that was parsed from the add or edit commands.
     */
    private static class CustomSlashCommandInfo {
        public String name = null;
        public String description = null;
        public String answer = null;
        public boolean isPublic = false;

        /**
         * Parses custom slash command info from an interaction.
         *
         * @param options         The options passed to the interaction
         * @param optionExtractor The method to get an option from a list item
         * @param fieldName       The name of the field holding the ID in a list item
         */
        public static CustomSlashCommandInfo buildFromInteraction(JSONArray options, Function<JSONObject, JSONObject> optionExtractor, String fieldName) {
            CustomSlashCommandInfo info = new CustomSlashCommandInfo();

            for (Object item : options) {
                JSONObject itemObject = optionExtractor.apply((JSONObject) item);

                switch (itemObject.getString(fieldName)) {
                    case "name":
                        info.name = itemObject.getString("value");
                        break;
                    case "description":
                        info.description = itemObject.getString("value");
                        break;
                    case "answer":
                        info.answer = itemObject.getString("value");
                        break;
                    case "is_public":
                        info.isPublic = itemObject.getBoolean("value");
                        break;
                    case "is_public_string":
                        info.isPublic = Boolean.parseBoolean(itemObject.getString("value"));
                        break;
                }
            }

            return info;
        }

        public String getValidationError(String locale) {
            if (name.equals("addc") || name.equals("removec") || name.equals("editc") || name.equals("clist")) {
                return localizeMessage(locale,
                        ":x: You cannot name a command `addc`, `removec`, `editc` or `clist`! This would conflict with the built-in commands.",
                        ":x: Tu ne peux pas appeler ta commande `addc`, `removec`, `editc` ou `clist` ! Ces noms sont déjà utilisés par les commandes d'administration.");
            } else if (!name.matches("[a-z0-9_-]{1,32}")) {
                return localizeMessage(locale,
                        ":x: Slash command names can only contain lowercase letters, numbers, `_` or `-`.",
                        ":x: Les noms de commande slash ne peuvent contenir que des lettres minuscules, des chiffres, des `_` ou des `- .");
            }

            return null;
        }
    }

    /**
     * Handles adding a command to a server, based on the raw options values.
     */
    private void addCommand(long serverId, JSONArray options, String locale, HttpServletResponse resp) throws IOException {
        // this is a slash command: each option simply has a name and a value.
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromInteraction(options, o -> o, "name");

        info.answer = info.answer.replace("\\n", "\n");

        if (info.getValidationError(locale) != null) {
            respond(resp, true, info.getValidationError(locale));
        } else {
            try {
                addSlashCommand(serverId, info);

                respond(resp, true, localizeMessage(locale,
                        ":white_check_mark: The **/" + info.name + "** command was created.",
                        ":white_check_mark: La commande **/" + info.name + "** a été créée."));
            } catch (MaximumCommandsReachedException e) {
                respond(resp, true, localizeMessage(locale,
                        ":x: **You reached the maximum amount of commands you can create on this server!**\n" +
                                "The limit set by Discord is 100 commands. Delete other commands to be able to create new ones!",
                        ":x: **Tu as atteint le nombre maximum de commandes que tu peux créer sur ce serveur !**\n" +
                                "La limite fixée par Discord est de 100 commandes. Tu dois supprimer des commandes avant de pouvoir en créer de nouvelles !"));
            }
        }
    }

    /**
     * Handles removing a command from a server, based on a command name.
     */
    private void removeCommand(long serverId, String name, String locale, HttpServletResponse resp) throws IOException {
        String slashCommandPath = "custom_slash_commands/" + serverId + "/" + name + ".json";

        try (InputStream is = CloudStorageUtils.getCloudStorageInputStream(slashCommandPath)) {
            removeSlashCommand(serverId, slashCommandPath, is);
            respond(resp, true, localizeMessage(locale,
                    ":white_check_mark: The **/" + name + "** command was deleted.",
                    ":white_check_mark: La commande **/" + name + "** a été supprimée."));
        } catch (StorageException e) {
            handleStorageException(locale, resp, e);
        }
    }

    /**
     * Handles listing the slash commands for a server.
     */
    private void listCommands(long serverId, String locale, HttpServletResponse resp) throws IOException {
        String prefix = "custom_slash_commands/" + serverId + "/";

        Set<String> commands = new TreeSet<>();
        Page<Blob> blobs = storage.list("max480-random-stuff.appspot.com", Storage.BlobListOption.prefix(prefix));
        for (Blob blob : blobs.iterateAll()) {
            String commandName = blob.getName();
            commandName = commandName.substring(prefix.length(), commandName.lastIndexOf("."));
            commands.add(commandName);
        }

        if (commands.isEmpty()) {
            respond(resp, true, localizeMessage(locale,
                    "There is no custom slash command on this server yet.",
                    "Il n'y a pas encore de commande slash personnalisée sur ce serveur."));
        } else {
            String message = localizeMessage(locale,
                    "Here is the list of custom slash commands on this server:\n/" + String.join("\n/", commands),
                    "Voici la liste des commandes slash personnalisées sur ce serveur :\n/" + String.join("\n/", commands));
            if (message.length() > 2000) {
                message = message.substring(0, 1995) + "[...]";
            }
            respond(resp, true, message);
        }
    }

    /**
     * Allows to display the "edit slash command" form.
     */
    private void sendEditCommandForm(long serverId, String name, String locale, HttpServletResponse resp) throws IOException {
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
            responseData.put("title", localizeMessage(locale, "Edit – /" + name, "Modifier – /" + name));

            JSONArray componentData = new JSONArray();
            componentData.put(getComponentDataForTextInput("name", localizeMessage(locale, "Name", "Nom"), name, 32, false));
            componentData.put(getComponentDataForTextInput("description", localizeMessage(locale, "Description", "Description"), description, 100, false));
            componentData.put(getComponentDataForTextInput("answer", localizeMessage(locale, "Answer", "Réponse"), answer, 2000, true));
            componentData.put(getComponentDataForTextInput("is_public_string",
                    localizeMessage(locale, "Response is public (true or false)", "Réponse publique (true = oui, false = non)"),
                    isPublic ? "true" : "false", 5, false));
            responseData.put("components", componentData);

            response.put("data", responseData);
            logger.fine("Responding with: " + response.toString(2));
            resp.getWriter().write(response.toString());
        } catch (StorageException e) {
            handleStorageException(locale, resp, e);
        }
    }

    /**
     * Checks whether the StorageException that just happened is caused by a non-existing command,
     * and responds appropriately if this is the case.
     */
    private void handleStorageException(String locale, HttpServletResponse resp, StorageException e) throws IOException {
        if (e.getCode() == 404) {
            respond(resp, true, localizeMessage(locale,
                    ":x: The command you specified does not exist! Check that you spelled it correctly.",
                    ":x: La commande que tu as spécifiée n'existe pas ! Vérifie que tu as saisi le nom correctement."));
        } else {
            throw e;
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
    private void editCommand(long serverId, String oldName, JSONArray options, String locale, HttpServletResponse resp) throws IOException {
        // this is a modal: each of the options is in a text input on an action row
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromInteraction(options,
                o -> o.getJSONArray("components").getJSONObject(0), "custom_id");

        if (info.getValidationError(locale) != null) {
            respond(resp, true, info.getValidationError(locale));
        } else {
            String slashCommandPath = "custom_slash_commands/" + serverId + "/" + oldName + ".json";
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream(slashCommandPath)) {
                // since adding a command is an upsert, deleting first is only required if the name changed.
                if (!oldName.equals(info.name)) {
                    removeSlashCommand(serverId, slashCommandPath, is);
                }

                addSlashCommand(serverId, info);

                if (oldName.equals(info.name)) {
                    respond(resp, true, localizeMessage(locale,
                            ":white_check_mark: The **/" + info.name + "** command was edited.",
                            ":white_check_mark: La commande **/" + info.name + "** a été modifiée."));
                } else {
                    respond(resp, true, localizeMessage(locale,
                            ":white_check_mark: The **/" + oldName + "** command was edited, and is now called **/" + info.name + "**.",
                            ":white_check_mark: La commande **/" + oldName + "** a été modifiée, et s'appelle maintenant **/" + info.name + "**."));
                }
            } catch (StorageException e) {
                handleStorageException(locale, resp, e);
            } catch (MaximumCommandsReachedException e) {
                // this error is not expected here.
                throw new IOException(e);
            }
        }
    }

    /**
     * Handles adding a slash command on Cloud Storage and Discord given all the information about it.
     */
    private void addSlashCommand(long serverId, CustomSlashCommandInfo info) throws IOException, MaximumCommandsReachedException {
        String path = "custom_slash_commands/" + serverId + "/" + info.name + ".json";
        logger.info("Adding " + path);

        long commandId = CustomSlashCommandsManager.addSlashCommand(serverId, info.name, info.description);

        JSONObject storedData = new JSONObject();
        storedData.put("id", commandId);
        storedData.put("answer", info.answer);
        storedData.put("isPublic", info.isPublic);
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
            respond(resp, !isPublic, answer);
        }
    }

    /**
     * Responds to a slash command, in response to the HTTP request.
     */
    private void respond(HttpServletResponse responseStream, boolean isPrivate, String message) throws IOException {
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

    private static String localizeMessage(String locale, String english, String french) {
        if ("fr".equals(locale)) {
            return french;
        }

        return english;
    }
}
