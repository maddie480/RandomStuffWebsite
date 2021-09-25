package com.max480.randomstuff.gae.discord.gamescommands;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.max480.randomstuff.gae.Constants;
import com.max480.randomstuff.gae.discord.gamescommands.games.Connect4;
import com.max480.randomstuff.gae.discord.gamescommands.games.Minesweeper;
import com.max480.randomstuff.gae.discord.gamescommands.games.Reversi;
import com.max480.randomstuff.gae.discord.gamescommands.games.TicTacToe;
import com.max480.randomstuff.gae.discord.gamescommands.status.GameState;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This is the API that makes the Games Bot run.
 * ... so, yeah, the Games Bot is actually an outgoing webhook. The bot is a lie. It doesn't even keep any state.
 */
@WebServlet(name = "DiscordGamesInteractionManager", urlPatterns = {"/discord/games-bot"})
public class InteractionManager extends HttpServlet {
    private static final Logger logger = Logger.getLogger("InteractionManager");

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
                !new LazySodiumJava(new SodiumJava()).cryptoSignVerifyDetached(
                        hexStringToByteArray(signature),
                        signedStuff, signedStuff.length,
                        hexStringToByteArray(Constants.GAMES_BOT_PUBLIC_KEY))) {

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
            } else if (data.getInt("type") == 2) {
                // slash command
                String gameName = data.getJSONObject("data").getString("name");
                String selfId = getUserObject(data).getString("id");
                String interactionToken = data.getString("token");

                if (gameName.equals("minesweeper")) {
                    // user wants to start a Minesweeper game, this needs special handling since it has a parameter.
                    int bombCount = data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getInt("value");
                    if (bombCount <= 0 || bombCount > 81) {
                        // generating no bomb, negative bombs or more bombs than there are spots to put bombs = trouble
                        sendError(":x: Please specify a bomb count between 1 and 81!", resp);
                    } else {
                        // start a new game
                        Minesweeper minesweeper = new Minesweeper(bombCount);
                        Game newGame = new OnePlayerGame(minesweeper, Long.parseLong(selfId), resp,
                                (thisGame, message, possibleActions, response) -> onAnswer(thisGame, message, possibleActions, interactionToken, response, true, false));
                        newGame.startGame();
                    }
                } else {
                    // 2-player game (for now Minesweeper is the only 1-player game)
                    boolean isUserCommand = data.getJSONObject("data").getInt("type") == 2; // 1 = slash command, 2 = user command, 3 = message command
                    boolean isAgainstBot = !data.getJSONObject("data").has("options") || data.getJSONObject("data").getJSONArray("options").isEmpty();

                    if (isUserCommand) {
                        // for example turn "Play Connect 4" into "connect4".
                        gameName = userCommandToSlashCommand(gameName);
                    }

                    if (!isUserCommand && isAgainstBot) {
                        // player used a slash command with no parameter: they want to play against AI
                        pickLevelAgainstBot(resp, gameName, selfId);
                    } else {
                        // user pinged another user in the command, or used a user command.
                        String userid;
                        if (isUserCommand) {
                            // grab the user that was clicked on.
                            userid = data.getJSONObject("data").getString("target_id");
                        } else {
                            // grab the option for the slash command.
                            userid = data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value");
                        }

                        JSONObject resolvedUser = data.getJSONObject("data").getJSONObject("resolved").getJSONObject("users").getJSONObject(userid);
                        if (userid.equals(Constants.GAMES_BOT_CLIENT_ID)) {
                            // the user picked Games Bot. :thinking: So they definitely want to play against CPU.
                            pickLevelAgainstBot(resp, gameName, selfId);
                        } else if (resolvedUser.has("bot") && resolvedUser.getBoolean("bot")) {
                            // the picked user... is a bot!
                            sendError(":x: You cannot play against a bot other than Games Bot!\nTo play against CPU, "
                                    + (isUserCommand ? "use the slash command and " : "")
                                    + "do not mention anyone.", resp);
                        } else {
                            if (userid.equals(selfId)) {
                                // the user picked themselves, what?
                                sendError(":x: You cannot play against yourself. :thinking:", resp);
                            } else {
                                // start a game between two humans.
                                onGameStartAgainstHuman(
                                        gameName,
                                        Long.parseLong(selfId),
                                        Long.parseLong(userid),
                                        interactionToken,
                                        resp);
                            }
                        }
                    }
                }

            } else if (data.getInt("type") == 3) {
                // clicked a button or used a combo box
                String interactionToken = data.getString("token");
                long selfId = Long.parseLong(getUserObject(data).getString("id"));

                if (data.getJSONObject("data").getInt("component_type") == 3) {
                    // this is a combo box! the only one we have is difficulty select.
                    String[] dataCombo = data.getJSONObject("data").getString("custom_id").split("\\|");
                    String game = dataCombo[0];
                    long userId = Long.parseLong(dataCombo[1]);

                    if (selfId != userId) {
                        // a user ran the slash command, and another tried to pick a difficulty!
                        sendError(":x: You are not the one that asked to play! Use `/" + game + "` to start a game for yourself.", resp);
                    } else {
                        String valueSelected = data.getJSONObject("data").getJSONArray("values").getString(0);
                        onGameStartAgainstCPU(game, selfId, Integer.parseInt(valueSelected), interactionToken, resp);
                    }

                } else if (data.getJSONObject("data").getInt("component_type") == 2) {
                    // this is a button.
                    String action = data.getJSONObject("data").getString("custom_id");
                    onGameAction(action, selfId, interactionToken, resp);
                }
            }
        }
    }

    /**
     * Converts a user command name to a slash command name.
     */
    private String userCommandToSlashCommand(String name) {
        switch (name) {
            case "Play Connect 4":
                return "connect4";
            case "Play Reversi":
                return "reversi";
            case "Play Tic-Tac-Toe":
                return "tictactoe";
            default:
                throw new RuntimeException("Unknown game in user command: " + name);
        }
    }

    /**
     * Gets the user object from the incoming JSON data.
     * This can be user (in case of a DM) or member.user (in case of a guild message).
     *
     * @param data The incoming data
     * @return The user JSON object
     */
    private JSONObject getUserObject(JSONObject data) {
        if (data.has("member")) {
            return data.getJSONObject("member").getJSONObject("user");
        }
        return data.getJSONObject("user");
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
     * Sends the message to have the user pick the bot level.
     *
     * @param resp     The stream to respond to Discord's request
     * @param gameName The name of the game
     * @param userId   The ID of the user that wants to play
     * @throws IOException In case the answer could not be sent
     */
    private void pickLevelAgainstBot(HttpServletResponse resp, String gameName, String userId) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 4); // channel message with source

        JSONObject responseData = new JSONObject();
        response.put("data", responseData);

        responseData.put("content", "Select the CPU level for the game:");
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));

        int maxLevel = 6;
        if (gameName.equals("connect4")) {
            maxLevel = 8;
        }

        // create the combo box
        JSONObject comboBox = new JSONObject();
        comboBox.put("type", 3);
        comboBox.put("custom_id", gameName + "|" + userId);

        // fill it with the options
        JSONArray options = new JSONArray();
        for (int i = 0; i <= maxLevel; i++) {
            JSONObject emoji = new JSONObject();
            emoji.put("id", (String) null);

            JSONObject option = new JSONObject();
            option.put("label", Integer.toString(i));
            option.put("value", Integer.toString(i));
            option.put("emoji", emoji);

            double percent = (i - 1.) / (maxLevel - 1.);
            if (percent < 0.25) {
                emoji.put("name", "\uD83D\uDFE2");
            } else if (percent < 0.5) {
                emoji.put("name", "\uD83D\uDFE1");
            } else if (percent < 0.75) {
                emoji.put("name", "\uD83D\uDFE0");
            } else {
                emoji.put("name", "\uD83D\uDD34");
            }

            if (i == 0) {
                option.put("description", "Plays at random");
                emoji.put("name", "\uD83C\uDFB2");
            } else if (i == 1) {
                option.put("description", "Easiest");
            } else if (i == maxLevel) {
                if (gameName.equals("tictactoe")) {
                    option.put("description", "Hardest");
                } else {
                    option.put("description", "Hardest (thinks for > 1 minute)");
                    emoji.put("name", "⚠");
                }
            }
            options.put(option);
        }
        comboBox.put("options", options);

        // wrap it in an array
        JSONArray actionRowValue = new JSONArray();
        actionRowValue.put(comboBox);

        // wrap it in an action row
        JSONObject actionRow = new JSONObject();
        actionRow.put("type", 1);
        actionRow.put("components", actionRowValue);

        // wrap the action row in an array aaaaa
        JSONArray actionRows = new JSONArray();
        actionRows.put(actionRow);

        // then finally put the array in the request.
        responseData.put("components", actionRows);

        logger.fine("Responding with: " + response.toString(2));
        resp.getWriter().write(response.toString());
    }

    /**
     * Starts a game between two humans.
     *
     * @param game             The game name
     * @param sender           The person that started the game
     * @param challenger       The person that was pinged to play
     * @param interactionToken The token that can be used to edit the message that was posted
     * @param servletResponse  The stream to respond to Discord's request
     */
    private void onGameStartAgainstHuman(String game, Long sender, Long challenger, String interactionToken, HttpServletResponse servletResponse) {
        GameState startedGame = startGameFromName(game);

        Game newGame = new Game(startedGame, false, false, sender, challenger, 0, 0L, servletResponse,
                (thisGame, message, possibleActions, response) -> onAnswer(thisGame, message, possibleActions, interactionToken, response, true, false));
        newGame.startGame();
    }

    /**
     * Starts a game between a human and a CPU.
     *
     * @param game             The game name
     * @param sender           The person that started the game
     * @param cpuLevel         The CPU level
     * @param interactionToken The token that can be used to edit the message that was posted
     * @param servletResponse  The stream to respond to Discord's request
     */
    private void onGameStartAgainstCPU(String game, Long sender, int cpuLevel, String interactionToken, HttpServletResponse servletResponse) {
        GameState startedGame = startGameFromName(game);

        Game newGame = new Game(startedGame, false, true, sender, 890556635091697665L, cpuLevel, 0L, servletResponse,
                (thisGame, message, possibleActions, response) -> onAnswer(thisGame, message, possibleActions, interactionToken, response, false, false));
        newGame.startGame();
    }

    /**
     * Creates the game state based on a game name.
     *
     * @param game The game name
     * @return The state of the game
     */
    private GameState startGameFromName(String game) {
        GameState startedGame;
        switch (game) {
            case "tictactoe":
                startedGame = new TicTacToe(Math.random() < 0.5, 3);
                break;
            case "connect4":
                startedGame = new Connect4(Math.random() < 0.5);
                break;
            case "reversi":
                startedGame = new Reversi(Math.random() < 0.5);
                break;
            default:
                throw new RuntimeException("This is not a known game!");
        }
        return startedGame;
    }

    /**
     * Executes an action after someone clicked a button.
     *
     * @param action           The action to execute, contains the clicked button + the game state
     * @param callerId         The Discord ID of the person that clicked
     * @param interactionToken The token that can be used to edit the message that was posted
     * @param servletResponse  The stream to respond to Discord's request
     * @throws IOException In case an error occurs when communicating with Discord
     */
    private void onGameAction(String action, Long callerId, String interactionToken, HttpServletResponse servletResponse) throws IOException {
        // the form of the command is: [p or t][button action]|[serialized game state]
        String command = action.substring(1, action.indexOf("|"));
        if (action.startsWith("p")) {
            // "partial" action: a start of a command since the entire command didn't fit.
            Game game = Game.deserializeFromString(action.substring(action.indexOf("|") + 1), servletResponse,
                    (thisGame, message, possibleActions, response) ->
                            onAnswer(thisGame, message, possibleActions, interactionToken, response, false, !command.isEmpty()));

            if (!Objects.equals(game.getCurrentTurnPlayer(), callerId)) {
                sendError(":x: It's not your turn!", servletResponse);
            } else {
                game.playerTurn(command);
            }
        } else {
            // "complete" action: we can actually run that one to advance the game.
            Game game = Game.deserializeFromString(action.substring(action.indexOf("|") + 1), servletResponse,
                    (thisGame, message, possibleActions, response) ->
                            onAnswer(thisGame, message, possibleActions, interactionToken, response, false, false));

            if (!Objects.equals(game.getCurrentTurnPlayer(), callerId)) {
                sendError(":x: It's not your turn!", servletResponse);
            } else {
                game.applyCommand(command);
            }
        }
    }

    /**
     * Sends an error to the user privately.
     *
     * @param errorMessage   The text of the error to send
     * @param responseStream The stream to respond to Discord's request
     * @throws IOException In case of communications error with Discord
     */
    private void sendError(String errorMessage, HttpServletResponse responseStream) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 4); // response in channel

        JSONObject responseData = new JSONObject();
        responseData.put("content", errorMessage);
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
        responseData.put("flags", 1 << 6); // ephemeral
        response.put("data", responseData);

        logger.fine("Responding with: " + response.toString(2));
        responseStream.getWriter().write(response.toString());
    }

    /**
     * Called when a turn ends, when the status should be posted to Discord.
     *
     * @param game             The game's current status
     * @param message          The message to send
     * @param possibleActions  The actions the player can use to continue, can be empty
     * @param interactionToken The token that can be used to edit the message that was posted
     * @param responseStream   The stream to respond to Discord's request, can be empty if we already responded
     * @param first            Whether this is the first message posted about that game
     * @param hasBackButton    Whether a "back" button should be displayed to go back to the full options list
     */
    private void onAnswer(Game game, String message, List<String> possibleActions, String interactionToken, HttpServletResponse responseStream,
                          boolean first, boolean hasBackButton) {

        JSONObject response = new JSONObject();
        response.put("type", first ? 4 : 7); // post or update message, depending on whether we already have one

        JSONObject responseData = new JSONObject();
        responseData.put("content", message);

        // allow pinging the player if this is their turn
        JSONObject allowedMentions = new JSONObject();
        allowedMentions.put("parse", new JSONArray());
        if (game.getCurrentTurnPlayer() != null) {
            allowedMentions.put("users", new JSONArray("[\"" + game.getCurrentTurnPlayer() + "\"]"));
        }
        responseData.put("allowed_mentions", allowedMentions);

        if (possibleActions.isEmpty()) {
            // there is no component
            responseData.put("components", new JSONArray());
        } else {
            String gameState = game.serializeToString();

            // just make a map action -> [action] for now
            int length = possibleActions.stream().mapToInt(String::length).max().orElse(0);
            Map<String, List<String>> groups = possibleActions.stream()
                    .collect(Collectors.toMap(k -> k, Collections::singletonList));

            // make sure there are 10 buttons or fewer. Truncate options if there are more.
            while (length > 1 && groups.size() > 10) {
                length--;

                // for example, with length 1 and options C2, C3 and D1, we will end up with:
                // C => [C2, C3]
                // D => [D1]
                // so we can display two buttons: "C..." (submenu to pick C2 or C3) and "D1"
                final int thisLength = length;
                groups = possibleActions.stream()
                        .collect(Collectors.toMap(
                                k -> k.substring(0, thisLength),
                                Collections::singletonList,
                                (l1, l2) -> {
                                    ArrayList<String> list = new ArrayList<>(l1);
                                    list.addAll(l2);
                                    return list;
                                }));
            }

            // map those to buttons.
            List<JSONObject> buttons = new ArrayList<>();
            for (Map.Entry<String, List<String>> buttonData : groups.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {

                boolean isMultiButton = buttonData.getValue().size() > 1; // whether this button hides multiple choices

                JSONObject button = new JSONObject();
                button.put("type", 2); // ... well, button
                button.put("custom_id", (isMultiButton ? ('p' + buttonData.getKey()) : ('t' + buttonData.getValue().get(0))) // is this a "partial" choice?
                        // the choice itself
                        + '|' + gameState // ... then the entire game state. This way Discord stores it, so we don't have to.
                );
                button.put("style", 2); // secondary (gray)
                setupButtonLabel(button, isMultiButton ? buttonData.getKey() + "..." : buttonData.getValue().get(0));
                buttons.add(button);
            }
            if (hasBackButton) {
                JSONObject button = new JSONObject();
                button.put("type", 2); // ... well, button
                button.put("custom_id", "p|" + gameState); // we "picked" an empty option.
                button.put("style", 4); // danger (red)
                button.put("label", "Back");
                buttons.add(button);
            }

            // Discord wants the buttons to be inside an action row, so do that now.
            // we'd like to make action rows with 5 buttons max each.
            JSONArray actionRowComponents = new JSONArray();

            // while there are buttons to dispatch, create new action rows (we won't have more than 10 anyway).
            while (!buttons.isEmpty()) {
                JSONObject actionRow = new JSONObject();
                actionRow.put("type", 1); // action row...

                JSONArray actionRowContents = new JSONArray();
                for (int i = 0; i < 5 && !buttons.isEmpty(); i++) {
                    actionRowContents.put(buttons.remove(0));
                }

                actionRow.put("components", actionRowContents);
                actionRowComponents.put(actionRow);
            }

            responseData.put("components", actionRowComponents);
        }
        response.put("data", responseData);

        try {
            if (responseStream != null) {
                // we should respond to Discord's request
                logger.fine("Responding with: " + response.toString(2) + " to caller");
                responseStream.getWriter().write(response.toString());
            } else {
                // we should call Discord to edit the message, since we already responded.
                String url = "https://discord.com/api/v9/webhooks/" + Constants.GAMES_BOT_CLIENT_ID + "/" + interactionToken + "/messages/@original";

                logger.fine("Responding with: " + responseData.toString(2) + " to " + url);

                // Apache HttpClient because PATCH does not exist according to HttpURLConnection
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpPatch httpPatch = new HttpPatch(new URI(url));
                httpPatch.setHeader("User-Agent", "DiscordBot (https://max480-random-stuff.appspot.com/discord-bots/#games-bot, 1.0)");
                httpPatch.setEntity(new StringEntity(responseData.toString(), ContentType.APPLICATION_JSON));
                CloseableHttpResponse httpResponse = httpClient.execute(httpPatch);

                if (httpResponse.getStatusLine().getStatusCode() != 200) {
                    logger.severe("Discord responded with " + httpResponse.getStatusLine().getStatusCode() + " to our edit request!");
                }
            }
        } catch (IOException | URISyntaxException e) {
            logger.severe("Error while communicating with Discord!");
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets up a button label, which is most of the time just... putting the label, but can sometimes involve some formatting.
     *
     * @param button The button
     * @param label  The label
     */
    private void setupButtonLabel(JSONObject button, String label) {
        // "dig", "mark" and "unmark" in Minesweeper can be replaced with emoji.
        String emoji = null;
        if (label.startsWith("Dig   ")) {
            emoji = "⛏";
            label = label.substring(6);
        } else if (label.startsWith("Mark  ")) {
            emoji = "❕";
            label = label.substring(6);
        } else if (label.startsWith("Unmark")) {
            emoji = "❌";
            label = label.substring(6).trim();
        }

        if (emoji != null) {
            JSONObject emojiObject = new JSONObject();
            emojiObject.put("id", (String) null);
            emojiObject.put("name", emoji);
            button.put("emoji", emojiObject);
        }

        button.put("label", label);
    }
}
