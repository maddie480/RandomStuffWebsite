package ovh.maddie480.randomstuff.frontend.discord.gamescommands;

import jakarta.servlet.http.HttpServletResponse;
import ovh.maddie480.randomstuff.frontend.discord.gamescommands.status.GameState;

import java.util.List;

/**
 * Adapter for a one-player game, in order to expose a constructor that takes 1 player.
 */
public class OnePlayerGame extends Game {
    /**
     * Prepares or restores a game.
     *
     * @param gameState      The state of the game
     * @param playerId       The ID of the player
     * @param response       The stream that can be used to respond to Discord
     * @param actionCallback The action to call when a turn is over
     */
    public OnePlayerGame(GameState gameState, Long playerId, HttpServletResponse response, MultiConsumer<Game, String, List<String>, HttpServletResponse> actionCallback) {
        super(gameState, false, false, playerId, playerId, 0, playerId, response, actionCallback);
    }
}
