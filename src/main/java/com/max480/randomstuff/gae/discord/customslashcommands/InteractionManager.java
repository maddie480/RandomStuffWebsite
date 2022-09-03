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
import java.util.Locale;
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

        String locale = data.getString("locale");

        if (data.getInt("type") == 5) {
            // edit form submit
            String[] customIdCut = data.getJSONObject("data").getString("custom_id").split("\\|");
            long serverId = Long.parseLong(customIdCut[0]);
            String commandName = customIdCut[1];

            editCommand(serverId, commandName, data.getJSONObject("data").getJSONArray("components"), locale, resp);
        } else {
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
                        sendEditCommandForm(serverId, data.getJSONObject("data").getJSONArray("options"), locale, resp);
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
                respond(resp, localizeMessage(locale,
                        ":x: An unexpected error occurred. Reach out at <https://discord.gg/59ztc8QZQ7> if this keeps happening!",
                        ":x: Une erreur inattendue est survenue. Signale-la sur <https://discord.gg/59ztc8QZQ7> si ça continue à arriver !"));
            }
        }
    }

    /**
     * Represents slash command info that was parsed from the add or edit commands.
     */
    private static class CustomSlashCommandInfo {
        public Long id = null;
        public long serverId = -1;

        public String name = null;
        public String description = null;

        public String answer = null;
        public String embedThumbnail = null;
        public String embedTitle = null;
        public String embedText = null;
        public String embedImage = null;
        public String embedColor = null;
        public boolean isPublic = false;

        /**
         * Parses custom slash command info from an interaction.
         *
         * @param options         The options passed to the interaction
         * @param optionExtractor The method to get an option from a list item
         * @param fieldName       The name of the field holding the ID in a list item
         */
        public static CustomSlashCommandInfo buildFromInteraction(long serverId, JSONArray options, Function<JSONObject, JSONObject> optionExtractor, String fieldName) {
            CustomSlashCommandInfo info = new CustomSlashCommandInfo();

            info.serverId = serverId;

            for (Object item : options) {
                JSONObject itemObject = optionExtractor.apply((JSONObject) item);

                // consider that empty fields do not exist at all.
                if (itemObject.get("value") instanceof String && itemObject.getString("value").isEmpty()) {
                    continue;
                }

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
                    case "embed_thumbnail":
                        info.embedThumbnail = itemObject.getString("value");
                        break;
                    case "embed_title":
                        info.embedTitle = itemObject.getString("value");
                        break;
                    case "embed_text":
                        info.embedText = itemObject.getString("value");
                        break;
                    case "embed_image":
                        info.embedImage = itemObject.getString("value");
                        break;
                    case "embed_color":
                        // we accept uppercase and lowercase hex, with or without #, but we standardize it to only keep uppercase without #.
                        info.embedColor = itemObject.getString("value").toUpperCase(Locale.ROOT);
                        if (info.embedColor.startsWith("#")) {
                            info.embedColor = info.embedColor.substring(1);
                        }
                        break;
                }
            }

            return info;
        }

        /**
         * Reads custom slash command info from Google Cloud Storage.
         */
        public static CustomSlashCommandInfo buildFromCloudStorage(long serverId, String commandName, boolean fetchDescription) throws IOException {
            CustomSlashCommandInfo info = new CustomSlashCommandInfo();

            info.serverId = serverId;
            info.name = commandName;

            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream(info.getCloudStoragePath())) {
                JSONObject commandInfo = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                info.id = commandInfo.getLong("id");
                info.isPublic = commandInfo.getBoolean("isPublic");

                if (fetchDescription) {
                    info.description = CustomSlashCommandsManager.getSlashCommandDescription(serverId, info.id);
                }

                if (commandInfo.has("answer")) info.answer = commandInfo.getString("answer");
                if (commandInfo.has("embedThumbnail")) info.embedThumbnail = commandInfo.getString("embedThumbnail");
                if (commandInfo.has("embedText")) info.embedText = commandInfo.getString("embedText");
                if (commandInfo.has("embedTitle")) info.embedTitle = commandInfo.getString("embedTitle");
                if (commandInfo.has("embedImage")) info.embedImage = commandInfo.getString("embedImage");
                if (commandInfo.has("embedColor")) info.embedColor = commandInfo.getString("embedColor");

                return info;
            }
        }

        public String getValidationError(String locale) {
            if (name.equals("addc") || name.equals("removec") || name.equals("editc") || name.equals("clist")) {
                return localizeMessage(locale,
                        ":x: You cannot name a command `addc`, `removec`, `editc` or `clist`! This would conflict with the built-in commands.",
                        ":x: Tu ne peux pas appeler ta commande `addc`, `removec`, `editc` ou `clist` ! Ces noms sont déjà utilisés par les commandes d'administration.");
            }

            if (!name.matches("[a-z0-9_-]{1,32}")) {
                return localizeMessage(locale,
                        ":x: Slash command names can only contain lowercase letters, numbers, `_` or `-`.",
                        ":x: Les noms de commande slash ne peuvent contenir que des lettres minuscules, des chiffres, des `_` ou des `-`.");
            }

            if (embedThumbnail != null && !embedThumbnail.matches("^https?://.*$")) {
                return localizeMessage(locale,
                        ":x: The embed thumbnail must be a link to an image!",
                        ":x: La miniature de l'intégration doit être un lien vers une image !");
            }

            if (embedImage != null && !embedImage.matches("^https?://.*$")) {
                return localizeMessage(locale,
                        ":x: The embed image must be a link to an image!",
                        ":x: L'image de l'intégration doit être un lien vers une image !");
            }

            if (embedColor != null && !embedColor.matches("^[0-9A-F]{6}$")) {
                return localizeMessage(locale,
                        ":x: The embed color must be a hex code (for instance `FF0000` for red)!\n" +
                                "You can use Google's color picker and copy the \"HEX\" field for example: <https://www.google.com/search?q=color+picker>",
                        ":x: La couleur de l'intégration doit être un code héxadécimal (par exemple `FF0000` pour du rouge) !\n" +
                                "Par exemple, tu peux utiliser le sélecteur de couleur de Google et copier la valeur \"HEX\" : <https://www.google.fr/search?q=color+picker>");
            }

            if (answer == null && embedTitle == null && embedText == null && embedImage == null) {
                return localizeMessage(locale,
                        ":x: Please specify what the slash command should answer!\nEither provide some text, or content to put in the embed.",
                        ":x: Tu dois préciser ce que la commande slash doit répondre !\nFournis du texte ou des informations pour construire une intégration.");
            }

            return null;
        }

        public String getCloudStoragePath() {
            return "custom_slash_commands/" + serverId + "/" + name + ".json";
        }
    }

    /**
     * Handles adding a command to a server, based on the raw options values.
     */
    private void addCommand(long serverId, JSONArray options, String locale, HttpServletResponse resp) throws IOException {
        // this is a slash command: each option simply has a name and a value.
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromInteraction(serverId, options, o -> o, "name");

        if (info.answer != null) info.answer = info.answer.replace("\\n", "\n");
        if (info.embedText != null) info.embedText = info.embedText.replace("\\n", "\n");

        if (info.getValidationError(locale) != null) {
            respond(resp, info.getValidationError(locale));
        } else {
            try {
                addSlashCommand(serverId, info);

                respond(resp, localizeMessage(locale,
                        ":white_check_mark: The **/" + info.name + "** command was created.",
                        ":white_check_mark: La commande **/" + info.name + "** a été créée."));
            } catch (MaximumCommandsReachedException e) {
                respond(resp, localizeMessage(locale,
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
        try {
            CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromCloudStorage(serverId, name, false);
            removeSlashCommand(serverId, info);
            respond(resp, localizeMessage(locale,
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
            respond(resp, localizeMessage(locale,
                    "There is no custom slash command on this server yet.",
                    "Il n'y a pas encore de commande slash personnalisée sur ce serveur."));
        } else {
            String message = localizeMessage(locale,
                    "Here is the list of custom slash commands on this server:\n/" + String.join("\n/", commands),
                    "Voici la liste des commandes slash personnalisées sur ce serveur :\n/" + String.join("\n/", commands));
            if (message.length() > 2000) {
                message = message.substring(0, 1995) + "[...]";
            }
            respond(resp, message);
        }
    }

    /**
     * Allows to display the "edit slash command" form.
     */
    private void sendEditCommandForm(long serverId, JSONArray options, String locale, HttpServletResponse resp) throws IOException {
        String name = null;
        boolean isEmbed = false;

        for (Object item : options) {
            JSONObject itemObject = (JSONObject) item;

            switch (itemObject.getString("name")) {
                case "name":
                    name = itemObject.getString("value");
                    break;
                case "edit":
                    isEmbed = "embed".equals(itemObject.getString("value"));
                    break;
            }
        }

        try {
            // get info on the current command
            CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromCloudStorage(serverId, name, !isEmbed);

            // build the response
            JSONObject response = new JSONObject();
            response.put("type", 9); // modal

            JSONObject responseData = new JSONObject();
            responseData.put("custom_id", serverId + "|" + name);
            responseData.put("title", localizeMessage(locale, "Edit – /" + name, "Modifier – /" + name));

            JSONArray componentData = new JSONArray();

            if (isEmbed) {
                componentData.put(getComponentDataForTextInput("embed_title", localizeMessage(locale, "Embed title", "Titre de l'intégration"), info.embedTitle, 256, false, false));
                componentData.put(getComponentDataForTextInput("embed_text", localizeMessage(locale, "Embed text", "Texte de l'intégration"), info.embedText, 4000, false, true));
                componentData.put(getComponentDataForTextInput("embed_image", localizeMessage(locale, "Embed image", "Image de l'intégration"), info.embedImage, 2000, false, false));
                componentData.put(getComponentDataForTextInput("embed_thumbnail", localizeMessage(locale, "Embed thumbnail", "Miniature de l'intégration"), info.embedThumbnail, 2000, false, false));
                componentData.put(getComponentDataForTextInput("embed_color", localizeMessage(locale, "Embed color", "Couleur de l'intégration"), info.embedColor, 7, false, false));
            } else {
                componentData.put(getComponentDataForTextInput("name", localizeMessage(locale, "Name", "Nom"), info.name, 32, true, false));
                componentData.put(getComponentDataForTextInput("description", localizeMessage(locale, "Description", "Description"), info.description, 100, true, false));
                componentData.put(getComponentDataForTextInput("answer", localizeMessage(locale, "Answer", "Réponse"), info.answer, 2000, false, true));
                componentData.put(getComponentDataForTextInput("is_public_string",
                        localizeMessage(locale, "Response is public (true or false)", "Réponse publique (true = oui, false = non)"),
                        info.isPublic ? "true" : "false", 5, true, false));
            }

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
            respond(resp, localizeMessage(locale,
                    ":x: The command you specified does not exist! Check that you spelled it correctly.",
                    ":x: La commande que tu as spécifiée n'existe pas ! Vérifie que tu as saisi le nom correctement."));
        } else {
            throw e;
        }
    }

    /**
     * Gives the JSON object describing a single text input.
     */
    private static JSONObject getComponentDataForTextInput(String id, String name, String value, long maxLength, boolean isRequired, boolean isLong) {
        JSONObject row = new JSONObject();
        row.put("type", 1); // ... action row

        JSONObject component = new JSONObject();
        component.put("type", 4); // text input
        component.put("custom_id", id);
        component.put("label", name);
        component.put("style", isLong ? 2 : 1);
        component.put("min_length", isRequired ? 1 : 0);
        component.put("max_length", maxLength);
        component.put("required", isRequired);
        component.put("value", value == null ? "" : value);

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
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromInteraction(serverId, options,
                o -> o.getJSONArray("components").getJSONObject(0), "custom_id");

        try {
            // if the command name is absent from the submitted form, this means we are editing the embed.
            boolean isEmbed = (info.name == null);

            CustomSlashCommandInfo oldInfo = CustomSlashCommandInfo.buildFromCloudStorage(serverId, oldName, isEmbed);

            if (isEmbed) {
                // retrieve non-embed info to keep it
                info.name = oldInfo.name;
                info.description = oldInfo.description;
                info.answer = oldInfo.answer;
                info.isPublic = oldInfo.isPublic;
            } else {
                // retrieve embed info to keep it
                info.embedTitle = oldInfo.embedTitle;
                info.embedText = oldInfo.embedText;
                info.embedImage = oldInfo.embedImage;
                info.embedThumbnail = oldInfo.embedThumbnail;
                info.embedColor = oldInfo.embedColor;
            }

            if (info.getValidationError(locale) != null) {
                respond(resp, info.getValidationError(locale));
            } else {
                // since adding a command is an upsert, deleting first is only required if the name changed.
                if (!oldName.equals(info.name)) {
                    removeSlashCommand(serverId, oldInfo);
                }

                addSlashCommand(serverId, info);

                if (oldName.equals(info.name)) {
                    respond(resp, localizeMessage(locale,
                            ":white_check_mark: The **/" + info.name + "** command was edited.",
                            ":white_check_mark: La commande **/" + info.name + "** a été modifiée."));
                } else {
                    respond(resp, localizeMessage(locale,
                            ":white_check_mark: The **/" + oldName + "** command was edited, and is now called **/" + info.name + "**.",
                            ":white_check_mark: La commande **/" + oldName + "** a été modifiée, et s'appelle maintenant **/" + info.name + "**."));
                }
            }
        } catch (StorageException e) {
            handleStorageException(locale, resp, e);
        } catch (MaximumCommandsReachedException e) {
            // this error is not expected here.
            throw new IOException(e);
        }
    }

    /**
     * Handles adding a slash command on Cloud Storage and Discord given all the information about it.
     */
    private void addSlashCommand(long serverId, CustomSlashCommandInfo info) throws IOException, MaximumCommandsReachedException {
        logger.info("Adding " + info.getCloudStoragePath());

        long commandId = CustomSlashCommandsManager.addSlashCommand(serverId, info.name, info.description);

        JSONObject storedData = new JSONObject();
        storedData.put("id", commandId);
        storedData.put("isPublic", info.isPublic);

        if (info.answer != null) storedData.put("answer", info.answer);
        if (info.embedTitle != null) storedData.put("embedTitle", info.embedTitle);
        if (info.embedImage != null) storedData.put("embedImage", info.embedImage);
        if (info.embedText != null) storedData.put("embedText", info.embedText);
        if (info.embedColor != null) storedData.put("embedColor", info.embedColor);
        if (info.embedThumbnail != null) storedData.put("embedThumbnail", info.embedThumbnail);

        CloudStorageUtils.sendBytesToCloudStorage(info.getCloudStoragePath(), "application/json", storedData.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Handles removing a slash command from Cloud Storage and Discord given all the information about it.
     */
    private void removeSlashCommand(long serverId, CustomSlashCommandInfo info) throws IOException {
        logger.info("Removing " + info.getCloudStoragePath());

        CustomSlashCommandsManager.removeSlashCommand(serverId, info.id);

        storage.delete(BlobId.of("max480-random-stuff.appspot.com", info.getCloudStoragePath()));
    }

    /**
     * Handles responding to one of the custom slash commands.
     */
    private void handleCommand(long serverId, String name, HttpServletResponse resp) throws IOException {
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromCloudStorage(serverId, name, false);

        JSONObject response = new JSONObject();
        response.put("type", 4); // response in channel

        JSONObject responseData = new JSONObject();

        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));

        if (info.answer != null) responseData.put("content", info.answer);

        if (!info.isPublic) {
            responseData.put("flags", 1 << 6); // ephemeral
        }

        JSONObject embed = new JSONObject();

        if (info.embedTitle != null) embed.put("title", info.embedTitle);
        if (info.embedText != null) embed.put("description", info.embedText);

        if (info.embedColor != null) {
            embed.put("color", Integer.parseInt(info.embedColor, 16));
        }

        if (info.embedImage != null) {
            JSONObject image = new JSONObject();
            image.put("url", info.embedImage);
            embed.put("image", image);
        }

        if (info.embedThumbnail != null) {
            JSONObject thumb = new JSONObject();
            thumb.put("url", info.embedThumbnail);
            embed.put("thumbnail", thumb);
        }

        if (!embed.isEmpty()) {
            JSONArray embeds = new JSONArray();
            embeds.put(embed);

            responseData.put("embeds", embeds);
        }

        response.put("data", responseData);

        logger.fine("Responding with: " + response.toString(2));
        resp.getWriter().write(response.toString());
    }

    /**
     * Responds privately to a slash command, in response to the HTTP request.
     */
    private void respond(HttpServletResponse responseStream, String message) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 4); // response in channel

        JSONObject responseData = new JSONObject();
        responseData.put("content", message);
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
        responseData.put("flags", 1 << 6); // ephemeral
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
