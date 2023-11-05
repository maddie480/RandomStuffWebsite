package ovh.maddie480.randomstuff.frontend.discord.gamescommands.status;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Adapter for one-player game states, that don't need to copy the game state and can just modify it.
 * (Copying is useful for CPU players, and there obviously isn't any here.)
 */
public abstract class OnePlayerGameState extends GameState {
    protected OnePlayerGameState() {
        super(true);
    }

    protected OnePlayerGameState(ObjectInputStream is) throws IOException {
        super(is);
    }

    @Override
    protected GameState copy() {
        throw new UnsupportedOperationException("One-player games do not need to copy!");
    }

    public abstract void applyCommandOnePlayer(String command);

    @Override
    public GameState applyCommand(String command) {
        applyCommandOnePlayer(command);
        setLatestCommand(command);
        return this;
    }
}
