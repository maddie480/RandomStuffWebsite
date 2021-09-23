package com.max480.randomstuff.gae.discord.gamescommands;


import com.max480.randomstuff.gae.discord.gamescommands.status.GameState;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A two-player game, with symmetric scores.
 */
public class Game {
    public interface MultiConsumer<T, U, V, W> {
        void accept(T t, U u, V v, W w);
    }

    private final Logger logger = Logger.getLogger("Game");

    private boolean player1IsPC = false;
    private boolean player2IsPC = false;

    private long player1Id = 0L;
    private long player2Id = 0L;

    private int cpuLevel;

    private GameState currentGameState;

    private long waitingInputFrom = 0L;

    private MultiConsumer<Game, String, List<String>, HttpServletResponse> actionCallback;
    private HttpServletResponse httpResponse;

    /**
     * Prepares or restores a game.
     *
     * @param gameState        The state of the game
     * @param player1IsPC      Whether player 1 is CPU controlled
     * @param player2IsPC      Whether player 2 is CPU controlled
     * @param player1Id        The Discord ID of player 1
     * @param player2Id        The Discord ID of player 2
     * @param cpuLevel         The level of the CPU players
     * @param waitingInputFrom The player that should play next, 0 means no-one (CPU)
     * @param actionCallback   The action to call when a turn is over
     * @param response         The stream that can be used to respond to Discord
     */
    public Game(GameState gameState, boolean player1IsPC, boolean player2IsPC, Long player1Id,
                Long player2Id, int cpuLevel, long waitingInputFrom, HttpServletResponse response,
                MultiConsumer<Game, String, List<String>, HttpServletResponse> actionCallback) {
        this.player1IsPC = player1IsPC;
        this.player2IsPC = player2IsPC;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.cpuLevel = cpuLevel;
        this.waitingInputFrom = waitingInputFrom;
        this.currentGameState = gameState;
        this.actionCallback = actionCallback;
        this.httpResponse = response;
    }

    /**
     * Starts a new game.
     */
    protected void startGame() {
        nextTurn();
    }

    /**
     * Runs a command to make the game progress.
     *
     * @param command The command to run
     */
    protected void applyCommand(String command) {
        currentGameState = currentGameState.applyCommand(command);
        nextTurn();
    }

    /**
     * Returns the player that is currently playing.
     *
     * @return The Discord ID of the player playing, or null if the CPU is playing.
     */
    protected Long getCurrentTurnPlayer() {
        return waitingInputFrom == 0L ? null : waitingInputFrom;
    }

    private void nextTurn() {
        logger.fine("Next turn.");

        if (!Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE, null).contains(currentGameState.scoreSituation())) {
            if ((currentGameState.isPlayer1Turn() && player1IsPC) || (!currentGameState.isPlayer1Turn() && player2IsPC)) {
                logger.fine("A CPU is playing.");

                // Update the message with "thinking..."
                waitingInputFrom = 0L;
                refreshStatus("I'm thinking...");

                // then asynchronously think!
                new Thread("Thinking...") {
                    @Override
                    public void run() {
                        computerTurn();
                    }
                }.start();
            } else {
                logger.fine("A player is playing.");
                playerTurn("");
            }
        } else if (currentGameState.scoreSituation() == null) {
            refreshStatus("Draw!");
        } else {
            Long winnerId = (currentGameState.scoreSituation() == Integer.MAX_VALUE ? player1Id : player2Id);
            refreshStatus("And the winner is... <@" + winnerId + ">!");
        }
    }

    /**
     * Handles a player turn: asks a command from the player.
     *
     * @param partialCommand The start of the command, in case we went in a submenu.
     */
    protected void playerTurn(String partialCommand) {
        waitingInputFrom = (currentGameState.isPlayer1Turn() ? player1Id : player2Id);
        logger.fine("We are now waiting for a command from " + waitingInputFrom);
        refreshStatus(currentGameState.getInstructions() + "\n<@" + waitingInputFrom + ">, it's your turn!", partialCommand);
    }

    private void refreshStatus(String line) {
        refreshStatus(line, "");
    }

    /**
     * Refreshes the status displayed on Discord.
     *
     * @param line           The status line to display below the game state
     * @param partialCommand The start of the command, in case we went in a submenu
     */
    private void refreshStatus(String line, String partialCommand) {
        String message = "```\n" + currentGameState.displayStatus() + "\n```\n" + line;
        if (currentGameState.getLatestCommand() != null) {
            message = "Last move: " + currentGameState.getLatestCommand() + "\n" + message;
        }

        // hide actions if CPU is playing, or if the game is over.
        boolean hideActions = waitingInputFrom == 0L || Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE, null).contains(currentGameState.scoreSituation());
        actionCallback.accept(this, message, hideActions ? Collections.emptyList() : currentGameState.listPossibleCommands()
                .stream().filter(command -> command.startsWith(partialCommand)).collect(Collectors.toList()), httpResponse);

        // now we responded to Discord, any further message will have to be a message edit request.
        httpResponse = null;
    }

    /**
     * Handles a CPU turn: seeks for the best command to apply, then applies it.
     * Ran in a new thread.
     */
    private void computerTurn() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        String chosenCommand = pickTheBestCommand();

        currentGameState = currentGameState.applyCommand(chosenCommand);
        nextTurn();
    }

    /**
     * Seeks for the best command to apply
     *
     * @return The best command that was found
     */
    private String pickTheBestCommand() {
        if (cpuLevel == 0) {
            // Level 0 is literally random. Pretend we think for a bit
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<String> commands = currentGameState.listPossibleCommands();
            Collections.shuffle(commands);
            return commands.get(0);
        } else {
            GameState bestStatus = null;
            int bestScore;

            List<GameState> possibleStatuses = currentGameState.listPossibleCommands().stream().map(currentGameState::applyCommand).collect(Collectors.toList());

            if (currentGameState.isPlayer1Turn()) {
                bestScore = Integer.MIN_VALUE;
                for (GameState leaf : possibleStatuses) {
                    GameState candidateStatus = minimax(leaf, cpuLevel - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    Integer candidateStatusScore = candidateStatus.scoreSituation();
                    if (candidateStatusScore == null) candidateStatusScore = 0;
                    logger.fine("Command " + leaf.getLatestCommand() + " gives a score of " + candidateStatusScore + " with an immediate score of " + leaf.scoreSituation());

                    if (bestScore < candidateStatusScore ||
                            (bestScore == candidateStatusScore && (bestStatus == null || bestStatus.scoreSituationNonNullable() < leaf.scoreSituationNonNullable()))) {

                        logger.fine("This is better");
                        bestScore = candidateStatusScore;
                        bestStatus = leaf;
                    }
                }
            } else {
                bestScore = Integer.MAX_VALUE;
                for (GameState leaf : possibleStatuses) {
                    GameState candidateStatus = minimax(leaf, cpuLevel - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    Integer candidateStatusScore = candidateStatus.scoreSituation();
                    if (candidateStatusScore == null) candidateStatusScore = 0;
                    logger.fine("Command " + leaf.getLatestCommand() + " gives a score of " + candidateStatusScore + " with an immediate score of " + leaf.scoreSituation());

                    if (bestScore > candidateStatusScore ||
                            (bestScore == candidateStatusScore && (bestStatus == null || bestStatus.scoreSituationNonNullable() > leaf.scoreSituationNonNullable()))) {

                        logger.fine("This is better");
                        bestScore = candidateStatusScore;
                        bestStatus = leaf;
                    }
                }
            }

            logger.fine("I'm picking " + bestStatus.getLatestCommand());
            return bestStatus.getLatestCommand();
        }
    }

    /**
     * Minimax with alpha-beta optimization
     *
     * @param node  The current game state
     * @param depth The remaining depth
     * @param alpha alpha
     * @param beta  beta
     * @return The best game state of this tree
     */
    private static GameState minimax(GameState node, int depth, int alpha, int beta) {
        // this slows down minimax by quite a lot, but this prevents the entire resources of the website from going
        // to that task, so that's a good thing!
        try {
            Thread.sleep(0, 10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (depth == 0) {
            // we shouldn't go deeper!
            return node;
        } else if (Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE, null).contains(node.scoreSituation())) {
            // this is an endgame situation
            return node;
        } else {
            GameState bestStatus = null;
            int bestScore;

            List<GameState> possibleStatuses = node.listPossibleCommands().stream().map(node::applyCommand).collect(Collectors.toList());

            if (node.isPlayer1Turn()) {
                bestScore = Integer.MIN_VALUE;
                for (GameState leaf : possibleStatuses) {
                    GameState candidateStatus = minimax(leaf, depth - 1, alpha, beta);
                    Integer candidateStatusScore = candidateStatus.scoreSituation();
                    if (candidateStatusScore == null) candidateStatusScore = 0;
                    if (bestScore < candidateStatusScore ||
                            (bestScore == candidateStatusScore && (bestStatus == null || bestStatus.scoreSituationNonNullable() < leaf.scoreSituationNonNullable()))) {

                        bestScore = candidateStatusScore;
                        bestStatus = candidateStatus;
                    }
                    alpha = Math.max(alpha, candidateStatusScore);
                    if (beta <= alpha) {
                        break;
                    }
                }
            } else {
                bestScore = Integer.MAX_VALUE;
                for (GameState leaf : possibleStatuses) {
                    GameState candidateStatus = minimax(leaf, depth - 1, alpha, beta);
                    Integer candidateStatusScore = candidateStatus.scoreSituation();
                    if (candidateStatusScore == null) candidateStatusScore = 0;
                    if (bestScore > candidateStatusScore ||
                            (bestScore == candidateStatusScore && (bestStatus == null || bestStatus.scoreSituationNonNullable() > leaf.scoreSituationNonNullable()))) {

                        bestScore = candidateStatusScore;
                        bestStatus = candidateStatus;
                    }
                    beta = Math.min(beta, candidateStatusScore);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }

            return bestStatus;
        }
    }

    @Override
    public String toString() {
        return "Game{" +
                "player1IsPC=" + player1IsPC +
                ", player2IsPC=" + player2IsPC +
                ", player1Id='" + player1Id + '\'' +
                ", player2Id='" + player2Id + '\'' +
                ", cpuLevel=" + cpuLevel +
                ", currentGameState=" + currentGameState +
                ", waitingInputFrom='" + waitingInputFrom + '\'' +
                '}';
    }

    /**
     * Called to turn the entire game into a base64 string.
     * This should not exceed 70 bytes.
     * <p>
     * This allows to make Discord keep the game state itself through the action button custom ids.
     *
     * @return A base64 string with all game data in it
     */
    protected String serializeToString() {
        byte[] serializedResult;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeBoolean(player1IsPC);
                oos.writeBoolean(player2IsPC);
                oos.writeLong(player1Id);
                oos.writeLong(player2Id);
                oos.writeByte((byte) cpuLevel);
                oos.writeLong(waitingInputFrom);
                currentGameState.serialize(oos);
            }
            serializedResult = os.toByteArray();
        } catch (IOException e) {
            // an I/O exception while writing to memory? weird.
            // this should turn into a server error.
            throw new RuntimeException(e);
        }

        return Base64.getEncoder().encodeToString(serializedResult);
    }

    /**
     * Called to turn a base64 string back to a game.
     *
     * @param s              The base64 string
     * @param actionCallback The action to call to update the game status
     * @return The deserialized game
     */
    protected static Game deserializeFromString(String s, HttpServletResponse httpResponse,
                                                MultiConsumer<Game, String, List<String>, HttpServletResponse> actionCallback) {
        byte[] serializedResult = Base64.getDecoder().decode(s);
        try (ByteArrayInputStream is = new ByteArrayInputStream(serializedResult);
             ObjectInputStream ois = new ObjectInputStream(is)) {

            boolean player1IsPC = ois.readBoolean();
            boolean player2IsPC = ois.readBoolean();
            long player1Id = ois.readLong();
            long player2Id = ois.readLong();
            int cpuLevel = ois.readByte();
            long waitingInputFrom = ois.readLong();

            return new Game(GameState.deserialize(ois), player1IsPC, player2IsPC, player1Id, player2Id, cpuLevel,
                    waitingInputFrom, httpResponse, actionCallback);
        } catch (IOException e) {
            // this should turn into a server error.
            throw new RuntimeException(e);
        }
    }
}
