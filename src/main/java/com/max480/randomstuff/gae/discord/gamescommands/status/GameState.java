package com.max480.randomstuff.gae.discord.gamescommands.status;


import com.max480.randomstuff.gae.discord.gamescommands.games.Connect4;
import com.max480.randomstuff.gae.discord.gamescommands.games.Minesweeper;
import com.max480.randomstuff.gae.discord.gamescommands.games.Reversi;
import com.max480.randomstuff.gae.discord.gamescommands.games.TicTacToe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * One of the states the game can take (for example, pieces position).
 */
public abstract class GameState {
    private boolean player1Turn;
    private String latestCommand;

    /**
     * Lists all commands that are possible in this state.
     * <p>
     * There should **always** be commands here, even if this is "skip a turn",
     * unless the game is over.
     *
     * @return La liste des commandes possibles
     */
    public abstract List<String> listPossibleCommands();

    /**
     * Applies the given command (like moving a piece), and returns the new game state.
     *
     * @param command The command to apply
     * @return The new status after the command has been applied (in a new object)
     */
    public abstract GameState applyCommand(String command);

    /**
     * Copies all elements of the game that are specific to the game
     *
     * @return The copy
     */
    protected abstract GameState copy();

    /**
     * Copies all elements of the game (specific or not)
     *
     * @param latestCommand The command that is being applied
     * @return The copy
     */
    protected <T extends GameState> T copyStatus(String latestCommand) {
        T copie = (T) copy();
        copie.setPlayer1Turn(player1Turn);
        copie.setLatestCommand(latestCommand);
        return copie;
    }

    /**
     * Gives a text representation of the status.
     */
    public abstract String displayStatus();

    /**
     * Gives a score to the situation for player 1:
     * the more the situation is good for player 1, the higher the score is.
     * <p>
     * Should be Integer.MAX_VALUE or MIN_VALUE if a player won, or null in case of a draw.
     * <p>
     * For one-player games, return MAX_VALUE if the player won, MIN_VALUE if they lost, 0 if the game is ongoing.
     *
     * @return Le score de la situation
     */
    public abstract Integer scoreSituation();

    /**
     * {@link #scoreSituation()} except 0 is returned instead of null in case of a draw.
     *
     * @return
     * @see #scoreSituation()
     */
    public int scoreSituationNonNullable() {
        Integer i = scoreSituation();
        if (i == null) return 0;
        return i;
    }

    /**
     * @return true if it is player 1's turn
     */
    public boolean isPlayer1Turn() {
        return player1Turn;
    }

    /**
     * @return The latest command that was run (if any)
     */
    public String getLatestCommand() {
        return latestCommand;
    }

    /**
     * Switches players.
     */
    protected void switchPlayers() {
        player1Turn = !player1Turn;
    }

    void setPlayer1Turn(boolean player1Turn) {
        this.player1Turn = player1Turn;
    }

    public void setLatestCommand(String latestCommand) {
        this.latestCommand = latestCommand;
    }

    protected GameState(boolean player1Begins) {
        this.player1Turn = player1Begins;
    }

    /**
     * @return Instructions for the different commands
     */
    public abstract String getInstructions();

    /**
     * Serializes the game-specific state to the given output stream.
     *
     * @param os The output stream to write the game to.
     * @throws IOException If something goes wrong when serializing the game
     */
    protected abstract void serializeGame(ObjectOutputStream os) throws IOException;

    /**
     * Serializes the game to the given output stream.
     *
     * @param os The output stream to write the game to.
     * @throws IOException If something goes wrong when serializing the game
     */
    public void serialize(ObjectOutputStream os) throws IOException {
        // write a byte to identify which game we are playing.
        if (this instanceof TicTacToe) {
            os.writeByte((byte) 0);
        } else if (this instanceof Connect4) {
            os.writeByte((byte) 1);
        } else if (this instanceof Reversi) {
            os.writeByte((byte) 2);
        } else if (this instanceof Minesweeper) {
            os.writeByte((byte) 3);
        } else {
            throw new IOException("What is this game?");
        }

        // write the GameState status
        os.writeBoolean(player1Turn);
        os.writeBoolean(latestCommand != null);
        if (latestCommand != null) {
            os.writeUTF(latestCommand);
        }

        // write specific game stuff
        serializeGame(os);
    }

    /**
     * Creates a GameState by deserializing a game.
     * Implementations call this before deserializing their own stuff.
     *
     * @param is The input stream to read the game from
     * @throws IOException If an error occurs while deserializing the game
     */
    protected GameState(ObjectInputStream is) throws IOException {
        player1Turn = is.readBoolean();
        if (is.readBoolean()) {
            latestCommand = is.readUTF();
        }
    }

    /**
     * Creates a GameState by deserializing a game.
     *
     * @param is The input stream to read the game from
     * @throws IOException If an error occurs while deserializing the game
     */
    public static GameState deserialize(ObjectInputStream is) throws IOException {
        int game = is.readByte();
        switch (game) {
            case 0:
                return new TicTacToe(is);
            case 1:
                return new Connect4(is);
            case 2:
                return new Reversi(is);
            case 3:
                return new Minesweeper(is);
            default:
                throw new IOException("Tried to deserialize unknown game (id: " + game + ")");
        }
    }

    /**
     * Utility method to write up to 8 booleans to a single byte.
     *
     * @param os    The output stream to write to
     * @param bools The booleans to write
     * @throws IOException If an error occurs while writing
     */
    public static void writeBooleans(ObjectOutputStream os, boolean[] bools) throws IOException {
        byte b = 0;
        for (int i = 0; i < bools.length; i++) {
            b += (bools[i] ? 1 : 0) << i;
        }
        os.writeByte(b);
    }

    /**
     * Utility method to read back the booleans written by {@link #writeBooleans(ObjectOutputStream, boolean[])}
     *
     * @param is     The input stream to read from
     * @param output The array to fill with the read booleans
     * @throws IOException If an error occurs while readong
     */
    public static void readBooleans(ObjectInputStream is, boolean[] output) throws IOException {
        byte b = is.readByte();
        for (int i = 0; i < output.length; i++) {
            output[i] = (b & (1 << i)) != 0;
        }
    }
}
