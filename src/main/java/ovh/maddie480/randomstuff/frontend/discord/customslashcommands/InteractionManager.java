package ovh.maddie480.randomstuff.frontend.discord.customslashcommands;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.frontend.SecretConstants;
import ovh.maddie480.randomstuff.frontend.discord.DiscordProtocolHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ovh.maddie480.randomstuff.frontend.discord.customslashcommands.CustomSlashCommandsManager.MaximumCommandsReachedException;

/**
 * This is the API that makes Custom Slash Commands run.
 */
@WebServlet(name = "CustomSlashCommandsBot", urlPatterns = {"/discord/custom-slash-commands"}, loadOnStartup = 4)
public class InteractionManager extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(InteractionManager.class);
    public static final String COMMAND_NAME_REGEX = "[a-z0-9_-]{1,32}";

    @Override
    public void init() {
        DiscordProtocolHandler.warmup();
        CustomSlashCommandsManager.warmupAuthentication();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject data = DiscordProtocolHandler.validateRequest(req, resp, SecretConstants.CUSTOM_SLASH_COMMANDS_PUBLIC_KEY);
        if (data == null) return;

        String locale = data.getString("locale");

        if (data.getInt("type") == 5) {
            String interactionId = data.getJSONObject("data").getString("custom_id");
            if (interactionId.contains("|")) {
                // edit form submit
                String[] customIdCut = interactionId.split("\\|");
                long serverId = Long.parseLong(customIdCut[0]);
                String commandName = customIdCut[1];

                editCommand(serverId, commandName, data.getJSONObject("data").getJSONArray("components"), locale, resp);
            } else {
                // create form submit
                long serverId = Long.parseLong(interactionId);
                addCommandWithForm(serverId, data.getJSONObject("data").getJSONArray("components"), locale, resp);
            }
        } else if (data.getInt("type") == 4) {
            // autocomplete
            long serverId = Long.parseLong(data.getString("guild_id"));
            commandAutocomplete(serverId, data, resp);
        } else {
            // slash command invocation
            try {
                String commandName = data.getJSONObject("data").getString("name");
                long serverId = Long.parseLong(data.getString("guild_id"));

                switch (commandName) {
                    case "addc" ->
                        // admin command: add a command to the server
                            addCommandWithSlash(serverId, data.getJSONObject("data").getJSONArray("options"), locale, resp);
                    case "removec" ->
                        // admin command: remove a command from the server
                            removeCommand(serverId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale, resp);
                    case "editc" ->
                        // admin command: show the modal allowing to edit a command
                            sendEditCommandForm(serverId, data.getJSONObject("data").getJSONArray("options"), locale, resp);
                    case "clist" ->
                        // admin command: show the modal allowing to edit a command
                            listCommands(serverId, locale, resp);
                    case "Turn into Custom Slash Command" ->
                        // message command: create a custom slash command from a message
                            createCustomSlashCommandFromMessage(serverId, data.getJSONObject("data"), locale, resp);
                    default ->
                        // another command: most definitely a custom configured one
                            handleCommand(serverId, commandName, resp);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("An unexpected error occurred!", e);
                respond(resp, localizeMessage(locale,
                        ":x: An unexpected error occurred. Reach out at <https://discord.gg/PdyfMaq9Vq> if this keeps happening!",
                        ":x: Une erreur inattendue est survenue. Signale-la sur <https://discord.gg/PdyfMaq9Vq> si ça continue à arriver !"));
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

                // string select with single item selected = text input with that value
                if (itemObject.has("values")) {
                    itemObject.put("value", itemObject.getJSONArray("values").getString(0));
                }

                // consider that empty fields do not exist at all.
                if (itemObject.get("value") instanceof String && itemObject.getString("value").isEmpty()) {
                    continue;
                }

                switch (itemObject.getString(fieldName)) {
                    case "name" -> info.name = itemObject.getString("value");
                    case "description" -> info.description = itemObject.getString("value");
                    case "answer" -> info.answer = itemObject.getString("value");
                    case "is_public" -> info.isPublic = itemObject.getBoolean("value");
                    case "is_public_string" ->
                            info.isPublic = Boolean.parseBoolean(itemObject.getString("value").toLowerCase(Locale.ROOT));
                    case "embed_thumbnail" -> info.embedThumbnail = itemObject.getString("value");
                    case "embed_title" -> info.embedTitle = itemObject.getString("value");
                    case "embed_text" -> info.embedText = itemObject.getString("value");
                    case "embed_image" -> info.embedImage = itemObject.getString("value");
                    case "embed_color" -> {
                        // we accept uppercase and lowercase hex, with or without #, but we standardize it to only keep uppercase without #.
                        info.embedColor = itemObject.getString("value").toUpperCase(Locale.ROOT);
                        if (info.embedColor.startsWith("#")) {
                            info.embedColor = info.embedColor.substring(1);
                        }
                    }
                }
            }

            return info;
        }

        /**
         * Reads custom slash command info from storage.
         */
        public static CustomSlashCommandInfo buildFromStorage(long serverId, String commandName, boolean fetchDescription) throws IOException {
            CustomSlashCommandInfo info = new CustomSlashCommandInfo();

            info.serverId = serverId;
            info.name = commandName;

            if (!Files.exists(info.getStoragePath())) {
                return null;
            }

            try (BufferedReader br = Files.newBufferedReader(info.getStoragePath())) {
                JSONObject commandInfo = new JSONObject(new JSONTokener(br));
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

            if (!name.matches(COMMAND_NAME_REGEX)) {
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
                                "To figure out this code, you can use [Google's color picker](https://www.google.com/search?q=color+picker) and copy the \"HEX\" field for example.",
                        ":x: La couleur de l'intégration doit être un code héxadécimal (par exemple `FF0000` pour du rouge) !\n" +
                                "Pour déterminer ce code, tu peux utiliser [le sélecteur de couleur de Google](https://www.google.fr/search?q=color+picker) et copier la valeur \"HEX\" par exemple.");
            }

            boolean noEmbed = embedTitle == null && embedText == null && embedImage == null;
            if (answer == null && noEmbed) {
                return localizeMessage(locale,
                        ":x: Please specify what the slash command should answer!\nEither provide some text, or content to put in the embed.",
                        ":x: Tu dois préciser ce que la commande slash doit répondre !\nFournis du texte ou des informations pour construire une intégration.");
            }

            if (embedThumbnail != null && noEmbed) {
                return localizeMessage(locale,
                        ":x: You must not define an embed thumbnail alone! Use `embed_image` instead.",
                        ":x: Tu ne peux pas utiliser seulement une miniature dans ton intégration ! Utilise `embed_image` à la place.");
            }

            if (embedColor != null && noEmbed) {
                return localizeMessage(locale,
                        ":x: You cannot have an embed color without having an embed!",
                        ":x: Tu ne peux pas définir une couleur d'intégration sans avoir d'intégration !");
            }

            return null;
        }

        public Path getStoragePath() {
            return Paths.get("/shared/discord-bots/custom-slash-commands/" + serverId + "/" + name + ".json");
        }
    }

    /**
     * Handles adding a command to a server, using the /addc command.
     */
    private void addCommandWithSlash(long serverId, JSONArray options, String locale, HttpServletResponse resp) throws IOException {
        // this is a slash command: each option simply has a name and a value.
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromInteraction(serverId, options, o -> o, "name");

        if (info.answer != null) info.answer = info.answer.replace("\\n", "\n");
        if (info.embedText != null) info.embedText = info.embedText.replace("\\n", "\n");

        createCommandFromInfo(serverId, locale, resp, info);
    }

    /**
     * Handles adding a command to a server, using the "create command" form.
     */
    private void addCommandWithForm(long serverId, JSONArray options, String locale, HttpServletResponse resp) throws IOException {
        // this is a modal: each of the options is in a text input on an action row
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromInteraction(serverId, options,
                o -> o.getJSONObject("component"), "custom_id");

        createCommandFromInfo(serverId, locale, resp, info);
    }

    /**
     * Creates a command from information coming from an interaction.
     */
    private void createCommandFromInfo(long serverId, String locale, HttpServletResponse resp, CustomSlashCommandInfo info) throws IOException {
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
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromStorage(serverId, name, false);

        if (info != null) {
            removeSlashCommand(serverId, info);
            respond(resp, localizeMessage(locale,
                    ":white_check_mark: The **/" + name + "** command was deleted.",
                    ":white_check_mark: La commande **/" + name + "** a été supprimée."));
        } else {
            respond(resp, localizeMessage(locale,
                    ":x: The command you specified does not exist! Check that you spelled it correctly.",
                    ":x: La commande que tu as spécifiée n'existe pas ! Vérifie que tu as saisi le nom correctement."));
        }
    }

    /**
     * Handles listing the slash commands for a server.
     */
    private void listCommands(long serverId, String locale, HttpServletResponse resp) throws IOException {
        Set<String> commands = listCommandsOnServer(serverId);

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

    private static Set<String> listCommandsOnServer(long serverId) throws IOException {
        Path directory = Paths.get("/shared/discord-bots/custom-slash-commands/" + serverId);
        if (!Files.isDirectory(directory)) return Collections.emptySet();

        try (Stream<Path> directoryListing = Files.list(directory)) {
            return directoryListing
                    .map(path -> path.getFileName().toString())
                    .map(commandName -> commandName.substring(0, commandName.lastIndexOf(".")))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private void commandAutocomplete(long serverId, JSONObject interaction, HttpServletResponse resp) throws IOException {
        String partialCommandName = null;
        for (Object o : interaction.getJSONObject("data").getJSONArray("options")) {
            JSONObject option = (JSONObject) o;
            if ("name".equals(option.getString("name"))) {
                partialCommandName = option.getString("value");
            }
        }

        JSONObject response = new JSONObject();
        response.put("type", 8); // autocomplete result

        JSONObject responseData = new JSONObject();
        JSONArray choices = new JSONArray();

        // if the command name isn't valid, no need to bother checking if it exists.
        if (partialCommandName.isEmpty() || partialCommandName.matches(COMMAND_NAME_REGEX)) {
            final String prefix = partialCommandName;

            listCommandsOnServer(serverId).stream()
                    .filter(l -> l.startsWith(prefix))
                    .sorted()
                    .limit(25)
                    .map(name -> {
                        JSONObject choice = new JSONObject();
                        choice.put("name", name);
                        choice.put("value", name);
                        return choice;
                    })
                    .forEach(choices::put);
        }

        responseData.put("choices", choices);
        response.put("data", responseData);

        log.debug("Responding with: {}", response.toString(2));
        response.write(resp.getWriter());
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
                case "name" -> name = itemObject.getString("value");
                case "edit" -> isEmbed = "embed".equals(itemObject.getString("value"));
            }
        }

        // get info on the current command
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromStorage(serverId, name, !isEmbed);

        if (info != null) {
            // build the response
            sendEditOrCreateForm(serverId, locale, resp, name, isEmbed, info, true);
        } else {
            respond(resp, localizeMessage(locale,
                    ":x: The command you specified does not exist! Check that you spelled it correctly.",
                    ":x: La commande que tu as spécifiée n'existe pas ! Vérifie que tu as saisi le nom correctement."));
        }
    }

    /**
     * Initiates the creation of a new custom slash command from a message.
     */
    private void createCustomSlashCommandFromMessage(long serverId, JSONObject interactionData, String locale, HttpServletResponse resp) throws IOException {
        JSONObject messageData = interactionData.getJSONObject("resolved").getJSONObject("messages").getJSONObject(interactionData.getString("target_id"));

        if (!messageData.getJSONArray("attachments").isEmpty()) {
            respond(resp, localizeMessage(locale,
                    """
                            :x: Custom slash command responses cannot contain attachments!
                            You can upload them somewhere and link to them instead.
                            If you want to include an image and text without the image link being visible, you can add a command with an embed image using </addc:992450514929332264>!""",
                    """
                            :x: Les réponses aux commandes slash personnalisées ne peuvent pas contenir de pièces jointes !
                            Tu peux les envoyer quelque part puis utiliser un lien à la place.
                            Si tu veux inclure une image et du texte sans que le lien de l'image soit visible, tu peux ajouter une commande avec une image intégrée en utilisant </addc:992450514929332264> !"""));
        } else if (messageData.getString("content").isEmpty()) {
            respond(resp, localizeMessage(locale,
                    ":x: This message has no text!",
                    ":x: Ce message n'a pas de texte !"));
        } else if (messageData.getString("content").length() > 2000) {
            respond(resp, localizeMessage(locale,
                    ":x: This message is too long! Slash command responses cannot exceed 2000 characters.",
                    ":x: Ce message est trop long ! Les réponses aux commandes slash ne peuvent pas dépasser 2000 caractères."));
        } else {
            CustomSlashCommandInfo partialInfo = new CustomSlashCommandInfo();
            partialInfo.answer = messageData.getString("content");
            sendEditOrCreateForm(serverId, locale, resp, null, false, partialInfo, false);
        }
    }

    /**
     * Displays the form allowing to edit or create a new command.
     */
    private static void sendEditOrCreateForm(long serverId, String locale, HttpServletResponse resp, String name, boolean isEmbed, CustomSlashCommandInfo info, boolean fillIsPublic) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 9); // modal

        JSONObject responseData = new JSONObject();

        if (name == null) {
            responseData.put("custom_id", String.valueOf(serverId));
            responseData.put("title", localizeMessage(locale, "New command", "Nouvelle commande"));
        } else {
            responseData.put("custom_id", serverId + "|" + name);
            responseData.put("title", localizeMessage(locale, "Edit – /" + name, "Modifier – /" + name));
        }

        JSONArray componentData = new JSONArray();

        if (isEmbed) {
            componentData.put(getComponentDataForTextInput("embed_title", localizeMessage(locale, "Embed title", "Titre de l'intégration"), info.embedTitle, 256, false, false));
            componentData.put(getComponentDataForTextInput("embed_text", localizeMessage(locale, "Embed text", "Texte de l'intégration"), info.embedText, 4000, false, true));
            componentData.put(getComponentDataForTextInput("embed_image", localizeMessage(locale, "Embed image", "Image de l'intégration"), info.embedImage, 2000, false, false));
            componentData.put(getComponentDataForTextInput("embed_thumbnail", localizeMessage(locale, "Embed thumbnail", "Miniature de l'intégration"), info.embedThumbnail, 2000, false, false));
            componentData.put(getComponentDataForTextInput("embed_color", localizeMessage(locale, "Embed color", "Couleur de l'intégration"), info.embedColor, 7, false, false));
        } else {
            boolean answerMandatory = (info.embedImage == null && info.embedText == null && info.embedTitle == null);

            componentData.put(getComponentDataForTextInput("name", localizeMessage(locale, "Name", "Nom"), info.name, 32, true, false));
            componentData.put(getComponentDataForTextInput("description", localizeMessage(locale, "Description", "Description"), info.description, 100, true, false));
            componentData.put(getComponentDataForTextInput("answer", localizeMessage(locale, "Answer", "Réponse"), info.answer, 2000, answerMandatory, true));
            componentData.put(getComponentDataForBooleanSelect(locale, "is_public_string",
                    localizeMessage(locale, "Response is public", "Réponse publique"),
                    fillIsPublic, info.isPublic));
        }

        responseData.put("components", componentData);

        response.put("data", responseData);
        log.debug("Responding with: {}", response.toString(2));
        response.write(resp.getWriter());
    }


    /**
     * Gives the JSON object describing a single text input.
     */
    private static JSONObject getComponentDataForTextInput(String id, String name, String value, long maxLength, boolean isRequired, boolean isLong) {
        JSONObject label = new JSONObject();
        label.put("type", 18); // label
        label.put("label", name);

        JSONObject component = new JSONObject();
        component.put("type", 4); // text input
        component.put("custom_id", id);
        component.put("style", isLong ? 2 : 1);
        component.put("min_length", isRequired ? 1 : 0);
        component.put("max_length", maxLength);
        component.put("required", isRequired);
        component.put("value", value == null ? "" : value);

        label.put("component", component);
        return label;
    }

    /**
     * Gives the JSON object describing a boolean select combo box.
     */
    private static JSONObject getComponentDataForBooleanSelect(String locale, String id, String name, boolean fillValue, boolean value) {
        JSONObject label = new JSONObject();
        label.put("type", 18); // label
        label.put("label", name);

        JSONObject component = new JSONObject();
        component.put("type", 3); // string select
        component.put("custom_id", id);
        component.put("required", true);
        component.put("min_values", 1);
        component.put("max_values", 1);

        JSONArray options = new JSONArray();

        JSONObject option = new JSONObject();
        option.put("label", localizeMessage(locale, "Yes", "Oui"));
        option.put("value", "true");
        option.put("default", fillValue && value);
        options.put(option);

        option = new JSONObject();
        option.put("label", localizeMessage(locale, "No", "Non"));
        option.put("value", "false");
        option.put("default", fillValue && !value);
        options.put(option);

        component.put("options", options);

        label.put("component", component);
        return label;
    }

    /**
     * Handles actually editing a slash command.
     */
    private void editCommand(long serverId, String oldName, JSONArray options, String locale, HttpServletResponse resp) throws IOException {
        // this is a modal: each of the options is in a text input on an action row
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromInteraction(serverId, options,
                o -> o.getJSONObject("component"), "custom_id");

        try {
            // if the command name is absent from the submitted form, this means we are editing the embed.
            boolean isEmbed = (info.name == null);

            CustomSlashCommandInfo oldInfo = CustomSlashCommandInfo.buildFromStorage(serverId, oldName, isEmbed);

            if (oldInfo == null) {
                respond(resp, localizeMessage(locale,
                        ":x: The command you specified does not exist! Check that you spelled it correctly.",
                        ":x: La commande que tu as spécifiée n'existe pas ! Vérifie que tu as saisi le nom correctement."));

                return;
            }

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
        } catch (MaximumCommandsReachedException e) {
            // this error is not expected here.
            throw new IOException(e);
        }
    }

    /**
     * Handles adding a slash command on storage and Discord given all the information about it.
     */
    private void addSlashCommand(long serverId, CustomSlashCommandInfo info) throws IOException, MaximumCommandsReachedException {
        log.info("Adding {}", info.getStoragePath());

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

        Files.createDirectories(info.getStoragePath().getParent());
        Files.writeString(info.getStoragePath(), storedData.toString());
    }

    /**
     * Handles removing a slash command from storage and Discord given all the information about it.
     */
    private void removeSlashCommand(long serverId, CustomSlashCommandInfo info) throws IOException {
        log.info("Removing {}", info.getStoragePath());

        CustomSlashCommandsManager.removeSlashCommand(serverId, info.id);

        Files.delete(info.getStoragePath());

        // if the server now has no command, delete the now-empty directory.
        File parent = info.getStoragePath().getParent().toFile();
        String[] fileList = parent.list();
        if (fileList != null && fileList.length == 0) {
            FileUtils.deleteDirectory(parent);
        }
    }

    /**
     * Handles responding to one of the custom slash commands.
     */
    private void handleCommand(long serverId, String name, HttpServletResponse resp) throws IOException {
        CustomSlashCommandInfo info = CustomSlashCommandInfo.buildFromStorage(serverId, name, false);

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

        log.debug("Responding with: {}", response.toString(2));
        response.write(resp.getWriter());
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

        log.debug("Responding with: {}", response.toString(2));
        response.write(responseStream.getWriter());
    }

    private static String localizeMessage(String locale, String english, String french) {
        if ("fr".equals(locale)) {
            return french;
        }

        return english;
    }
}
