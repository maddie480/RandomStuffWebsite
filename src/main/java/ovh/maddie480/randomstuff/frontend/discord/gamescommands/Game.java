package ovh.maddie480.randomstuff.frontend.discord.gamescommands;


import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.frontend.discord.gamescommands.status.GameState;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A two-player game, with symmetric scores.
 */
public class Game {
    public interface MultiConsumer<T, U, V, W> {
        void accept(T t, U u, V v, W w);
    }

    private static final Logger log = LoggerFactory.getLogger(Game.class);

    private final boolean player1IsPC;
    private final boolean player2IsPC;

    private final long player1Id;
    private final long player2Id;

    private final int cpuLevel;

    private GameState currentGameState;

    private long waitingInputFrom;

    private final MultiConsumer<Game, String, List<String>, HttpServletResponse> actionCallback;
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
        log.debug("Next turn.");

        if (!Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE, null).contains(currentGameState.scoreSituation())) {
            if ((currentGameState.isPlayer1Turn() && player1IsPC) || (!currentGameState.isPlayer1Turn() && player2IsPC)) {
                log.debug("A CPU is playing.");

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
                log.debug("A player is playing.");
                playerTurn("");
            }
        } else if (currentGameState.scoreSituation() == null) {
            refreshStatus("Draw!");
        } else {
            long winnerId = (currentGameState.scoreSituation() == Integer.MAX_VALUE ? player1Id : player2Id);
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
        log.debug("We are now waiting for a command from " + waitingInputFrom);
        refreshStatus(currentGameState.getInstructions() + "\n<@" + waitingInputFrom + ">, it's your turn!", partialCommand);
    }

    private void refreshStatus(String line) {
        refreshStatus(line, "");
    }

    /**
     * Refreshes the status displayed on Discord.
     *
     * @param line           The status line to display below the game state
     *                       (in case of one-player games, getInstructions() will be taken instead)
     * @param partialCommand The start of the command, in case we went in a submenu
     */
    private void refreshStatus(String line, String partialCommand) {
        String message = currentGameState.displayStatus() + "\n" +
                (player1Id == player2Id ? currentGameState.getInstructions() : line);

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

            List<GameState> possibleStatuses = currentGameState.listPossibleCommands().stream().map(currentGameState::applyCommand).toList();

            if (currentGameState.isPlayer1Turn()) {
                bestScore = Integer.MIN_VALUE;
                for (GameState leaf : possibleStatuses) {
                    GameState candidateStatus = minimax(leaf, cpuLevel - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    Integer candidateStatusScore = candidateStatus.scoreSituation();
                    if (candidateStatusScore == null) candidateStatusScore = 0;
                    log.debug("Command " + leaf.getLatestCommand() + " gives a score of " + candidateStatusScore + " with an immediate score of " + leaf.scoreSituation());

                    if (bestScore < candidateStatusScore ||
                            (bestScore == candidateStatusScore && (bestStatus == null || bestStatus.scoreSituationNonNullable() < leaf.scoreSituationNonNullable()))) {

                        log.debug("This is better");
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
                    log.debug("Command " + leaf.getLatestCommand() + " gives a score of " + candidateStatusScore + " with an immediate score of " + leaf.scoreSituation());

                    if (bestScore > candidateStatusScore ||
                            (bestScore == candidateStatusScore && (bestStatus == null || bestStatus.scoreSituationNonNullable() > leaf.scoreSituationNonNullable()))) {

                        log.debug("This is better");
                        bestScore = candidateStatusScore;
                        bestStatus = leaf;
                    }
                }
            }

            log.debug("I'm picking " + bestStatus.getLatestCommand());
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
        if (depth == 0) {
            // we shouldn't go deeper!
            return node;
        } else if (Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE, null).contains(node.scoreSituation())) {
            // this is an endgame situation
            return node;
        } else {
            GameState bestStatus = null;
            int bestScore;

            List<GameState> possibleStatuses = node.listPossibleCommands().stream().map(node::applyCommand).toList();

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
    public String serializeToString() {
        byte[] serializedResult;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeBoolean(player1Id == player2Id);
                if (player1Id == player2Id) {
                    // optimize storage!
                    oos.writeLong(player1Id);
                } else {
                    oos.writeBoolean(player1IsPC);
                    oos.writeBoolean(player2IsPC);
                    oos.writeLong(player1Id);
                    oos.writeLong(player2Id);
                    oos.writeLong(waitingInputFrom);
                    oos.writeByte((byte) cpuLevel);
                }
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
    public static Game deserializeFromString(String s, HttpServletResponse httpResponse,
                                             MultiConsumer<Game, String, List<String>, HttpServletResponse> actionCallback) {
        byte[] serializedResult = Base64.getDecoder().decode(s);
        try (ByteArrayInputStream is = new ByteArrayInputStream(serializedResult);
             ObjectInputStream ois = new ObjectInputStream(is)) {

            if (ois.readBoolean()) {
                // one-player game!
                long playerId = ois.readLong();
                return new OnePlayerGame(GameState.deserialize(ois), playerId, httpResponse, actionCallback);
            } else {
                boolean player1IsPC = ois.readBoolean();
                boolean player2IsPC = ois.readBoolean();
                long player1Id = ois.readLong();
                long player2Id = ois.readLong();
                long waitingInputFrom = ois.readLong();
                int cpuLevel = ois.readByte();

                return new Game(GameState.deserialize(ois), player1IsPC, player2IsPC, player1Id, player2Id, cpuLevel,
                        waitingInputFrom, httpResponse, actionCallback);
            }
        } catch (IOException e) {
            // this should turn into a server error.
            throw new RuntimeException(e);
        }
    }
}
