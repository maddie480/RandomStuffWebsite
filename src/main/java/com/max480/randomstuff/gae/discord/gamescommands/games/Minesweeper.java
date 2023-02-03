package com.max480.randomstuff.gae.discord.gamescommands.games;

import com.max480.randomstuff.gae.discord.gamescommands.status.GameState;
import com.max480.randomstuff.gae.discord.gamescommands.status.OnePlayerGameState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Minesweeper extends OnePlayerGameState {
    private static final Logger logger = Logger.getLogger("Minesweeper");

    private final int[][] map = new int[9][9]; // -1 = bomb, other = amount of bombs around
    private final int[][] reveal = new int[9][9]; // 0 = nothing, 1 = revealed, 2 = marked

    private long beginning;

    public Minesweeper(int bombCount) {
        super();

        logger.fine("Generating minesweeper grid with " + bombCount + " bombs");

        for (int i = 0; i < bombCount; i++) {
            while (true) {
                int x = (int) (Math.random() * 9);
                int y = (int) (Math.random() * 9);

                if (map[x][y] == 0) {
                    map[x][y] = -1;
                    break;
                }
            }
        }

        populateBombsAround();
        beginning = System.currentTimeMillis();

        logger.fine("Grid generated!");
    }

    @Override
    protected void serializeGame(ObjectOutputStream os) throws IOException {
        for (int i = 0; i < 9; i++) {
            GameState.writeBooleans(os, new boolean[]{
                    reveal[i][0] == 1, reveal[i][0] == 2,
                    reveal[i][1] == 1, reveal[i][1] == 2,
                    reveal[i][2] == 1, reveal[i][2] == 2,
                    reveal[i][3] == 1, reveal[i][3] == 2,
            });
            GameState.writeBooleans(os, new boolean[]{
                    reveal[i][4] == 1, reveal[i][4] == 2,
                    reveal[i][5] == 1, reveal[i][5] == 2,
                    reveal[i][6] == 1, reveal[i][6] == 2,
                    reveal[i][7] == 1, reveal[i][7] == 2,
            });
            GameState.writeBooleans(os, new boolean[]{
                    map[i][0] == -1, map[i][1] == -1,
                    map[i][2] == -1, map[i][3] == -1,
                    map[i][4] == -1, map[i][5] == -1,
                    reveal[i][8] == 1, reveal[i][8] == 2
            });
            GameState.writeBooleans(os, new boolean[]{
                    map[i][6] == -1, map[i][7] == -1,
                    map[i][8] == -1
            });
        }

        os.writeLong(beginning);
    }

    public Minesweeper(ObjectInputStream is) throws IOException {
        super(is);

        for (int i = 0; i < 9; i++) {
            boolean[] slice = new boolean[8];

            // read first 4 reveals
            GameState.readBooleans(is, slice);
            for (int j = 0; j < 4; j++) {
                reveal[i][j] = (slice[j * 2] ? 1 : (slice[j * 2 + 1] ? 2 : 0));
            }

            // read the following 4
            GameState.readBooleans(is, slice);
            for (int j = 0; j < 4; j++) {
                reveal[i][j + 4] = (slice[j * 2] ? 1 : (slice[j * 2 + 1] ? 2 : 0));
            }

            // read the last one + the 6 first maps
            GameState.readBooleans(is, slice);
            for (int j = 0; j < 6; j++) {
                map[i][j] = (slice[j] ? -1 : 0);
            }
            reveal[i][8] = (slice[6] ? 1 : (slice[7] ? 2 : 0));

            // read the 3 other maps
            slice = new boolean[3];
            GameState.readBooleans(is, slice);
            for (int j = 0; j < 3; j++) {
                map[i][j + 6] = (slice[j] ? -1 : 0);
            }
        }

        populateBombsAround();

        beginning = is.readLong();
    }

    private void populateBombsAround() {
        // bombs are in spots that are = -1.
        // populate spots around with the amount of bombs that are around.
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                if (map[x][y] != -1) {
                    int bombsAround = 0;

                    for (int sx = Math.max(0, x - 1); sx <= Math.min(8, x + 1); sx++) {
                        for (int sy = Math.max(0, y - 1); sy <= Math.min(8, y + 1); sy++) {
                            if (map[sx][sy] == -1) bombsAround++;
                        }
                    }

                    map[x][y] = bombsAround;
                }
            }
        }
    }

    private void appendDigitEmote(StringBuilder builder, int i) {
        switch (i) {
            case -1 -> builder.append(":x:");
            case 0 -> builder.append(":white_large_square:");
            case 1 -> builder.append(":one:");
            case 2 -> builder.append(":two:");
            case 3 -> builder.append(":three:");
            case 4 -> builder.append(":four:");
            case 5 -> builder.append(":five:");
            case 6 -> builder.append(":six:");
            case 7 -> builder.append(":seven:");
            case 8 -> builder.append(":eight:");
            case 9 -> builder.append(":nine:");
            default -> builder.append(":question:");
        }
    }

    private void digAndExtend(int x, int y) {
        logger.fine("Digging at " + x + ", " + y);

        reveal[x][y] = 1;

        if (map[x][y] == 0) {
            for (int sx = Math.max(0, x - 1); sx <= Math.min(8, x + 1); sx++) {
                for (int sy = Math.max(0, y - 1); sy <= Math.min(8, y + 1); sy++) {
                    if (reveal[sx][sy] != 1) {
                        digAndExtend(sx, sy);
                    }
                }
            }
        }
    }

    private boolean areAllBombsFound() {
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                if ((reveal[x][y] == 2) != (map[x][y] == -1)) return false;
            }
        }

        return true;
    }

    @Override
    public List<String> listPossibleCommands() {
        List<String> allCommands = new ArrayList<>();
        for (int i = 0; i < reveal.length; i++) {
            for (int j = 0; j < reveal[0].length; j++) {
                if (reveal[i][j] == 0) {
                    // we can either dig or mark.
                    allCommands.add("Dig   " + (i + 1) + "," + (j + 1));
                    allCommands.add("Mark  " + (i + 1) + "," + (j + 1));
                } else if (reveal[i][j] == 2) {
                    // we can un-mark.
                    allCommands.add("Unmark " + (i + 1) + "," + (j + 1));
                }
            }
        }
        return allCommands;
    }

    @Override
    public GameState applyCommand(String command) {
        // we can't afford to save the latest command due to space constraints!
        applyCommandOnePlayer(command);
        return this;
    }

    @Override
    public void applyCommandOnePlayer(String command) {
        Matcher markRegex = Pattern.compile("(?:Mark|Unmark) +([1-9]),([1-9])").matcher(command);
        Matcher digRegex = Pattern.compile("Dig +([1-9]),([1-9])").matcher(command);

        if (markRegex.matches()) {
            int x = Integer.parseInt(markRegex.group(1)) - 1;
            int y = Integer.parseInt(markRegex.group(2)) - 1;

            if (reveal[x][y] == 2) {
                logger.fine("Command for removing a mark on " + x + ", " + y);
                reveal[x][y] = 0;
            } else if (reveal[x][y] == 0) {
                logger.fine("Command for putting a mark on " + x + ", " + y);
                reveal[x][y] = 2;
            } else {
                logger.warning("Command for putting a mark on " + x + ", " + y + " is invalid because we dug there!");
            }
        }

        if (digRegex.matches()) {
            int x = Integer.parseInt(digRegex.group(1)) - 1;
            int y = Integer.parseInt(digRegex.group(2)) - 1;

            if (reveal[x][y] == 1) {
                logger.warning("Invalid command: we already dug on " + x + ", " + y);
            } else if (map[x][y] == -1) {
                logger.fine("Player fell on a bomb!");
                reveal[x][y] = 1;
            } else {
                digAndExtend(x, y);
            }
        }
    }

    @Override
    public String displayStatus() {
        StringBuilder builder = new StringBuilder(":white_small_square::one::two::three::four::five::six::seven::eight::nine:\n");

        int countBombs = 0, countMarkers = 0;

        for (int x = 0; x < 9; x++) {
            appendDigitEmote(builder, x + 1);

            for (int y = 0; y < 9; y++) {
                if (map[x][y] == -1) countBombs++;

                if (reveal[x][y] == 0) {
                    builder.append(":black_large_square:");
                } else if (reveal[x][y] == 1) {
                    // show the spot
                    appendDigitEmote(builder, map[x][y]);
                } else {
                    builder.append(":grey_exclamation:");
                    countMarkers++;
                }
            }

            builder.append("\n");
        }
        builder.append("\nBomb amount: ").append(countBombs).append(" / Marked spots: ").append(countMarkers);
        return builder.toString();
    }

    @Override
    public Integer scoreSituation() {
        if (areAllBombsFound()) {
            return Integer.MAX_VALUE;
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (reveal[i][j] == 1 && map[i][j] == -1) {
                    // we revealed a bomb! aaaa
                    return Integer.MIN_VALUE;
                }
            }
        }

        return 0;
    }

    @Override
    public String getInstructions() {
        if (scoreSituation() == Integer.MAX_VALUE) {
            long time = System.currentTimeMillis() - beginning;
            long minutes = time / 60000;
            long seconds = (time / 1000) % 60;
            long millis = time % 1000;
            return "Congratulations, you won! You took " + new DecimalFormat("00").format(minutes)
                    + ":" + new DecimalFormat("00").format(seconds) + "." + new DecimalFormat("000").format(millis) + ".";
        } else if (scoreSituation() == Integer.MIN_VALUE) {
            return ":bomb: You hit a bomb! Sorry, you lost.";

        }
        return "Format of the options: [line],[column]\n:pick: digs, :grey_exclamation: marks a bomb, :x: removes the mark";
    }

    public static Minesweeper generateRandom() {
        Minesweeper rng = new Minesweeper(0);
        Random r = new Random();
        rng.beginning = r.nextLong();
        for (int i = 0; i < rng.map.length; i++) {
            for (int j = 0; j < rng.map[0].length; j++) {
                rng.map[i][j] = r.nextBoolean() ? -1 : 0;
            }
        }
        for (int i = 0; i < rng.reveal.length; i++) {
            for (int j = 0; j < rng.reveal[0].length; j++) {
                rng.reveal[i][j] = r.nextInt(3);
            }
        }
        rng.populateBombsAround();
        return rng;
    }
}
