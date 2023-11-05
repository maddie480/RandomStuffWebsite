package ovh.maddie480.randomstuff.frontend.discord.gamescommands.games;

import ovh.maddie480.randomstuff.frontend.discord.gamescommands.status.GameState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TicTacToe extends GameState {
    private final Boolean[][] board;

    // size should be 3 or 4 (4 already doesn't work as a game so don't even try 5)
    public TicTacToe(boolean player1Starts, int size) {
        super(player1Starts);

        board = new Boolean[size][size];
    }

    @Override
    protected void serializeGame(ObjectOutputStream os) throws IOException {
        os.writeByte(board.length);
        for (int i = 0; i < board.length; i++) {
            boolean[] line = new boolean[board.length * 2];
            for (int j = 0; j < board.length; j++) {
                line[j * 2] = board[i][j] == null;
                line[j * 2 + 1] = board[i][j] == Boolean.TRUE;
            }
            GameState.writeBooleans(os, line);
        }
    }

    public TicTacToe(ObjectInputStream is) throws IOException {
        super(is);

        int size = is.readByte();
        board = new Boolean[size][size];
        for (int i = 0; i < board.length; i++) {
            boolean[] line = new boolean[board.length * 2];
            GameState.readBooleans(is, line);
            for (int j = 0; j < board.length; j++) {
                boolean isNull = line[j * 2];
                boolean isTrue = line[j * 2 + 1];
                board[i][j] = isNull ? null : isTrue;
            }
        }
    }

    @Override
    public List<String> listPossibleCommands() {
        List<String> commands = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                if (board[i][j] == null) {
                    commands.add((i + 1) + "," + (j + 1));
                }
            }
        }

        Collections.shuffle(commands);
        return commands;
    }

    @Override
    protected GameState copy() {
        TicTacToe status = new TicTacToe(true, board.length);

        // copy the board
        for (int i = 0; i < board.length; i++) {
            System.arraycopy(this.board[i], 0, status.board[i], 0, board.length);
        }
        return status;
    }

    @Override
    public GameState applyCommand(String command) {
        String[] splitted = command.split(",");
        int i = Integer.parseInt(splitted[0]) - 1;
        int j = Integer.parseInt(splitted[1]) - 1;

        TicTacToe newStatus = copyStatus(command);
        newStatus.board[i][j] = isPlayer1Turn();
        newStatus.switchPlayers();
        return newStatus;
    }

    @Override
    public String displayStatus() {
        StringBuilder stringBuilder = new StringBuilder("```\n");
        for (int i = 0; i < board.length; i++) {
            stringBuilder.append(".---".repeat(board.length));
            stringBuilder.append(".\n");
            for (int j = 0; j < board.length; j++) {
                stringBuilder.append("|");
                if (board[i][j] == Boolean.TRUE) {
                    stringBuilder.append(" X ");
                } else if (board[i][j] == Boolean.FALSE) {
                    stringBuilder.append(" O ");
                } else {
                    stringBuilder.append("   ");
                }
            }
            stringBuilder.append("|\n");
        }
        stringBuilder.append(".---".repeat(board.length));
        stringBuilder.append(".\n```");
        return stringBuilder.toString();
    }

    @Override
    public Integer scoreSituation() {
        int sum = 0;
        for (int i = 0; i < board.length - 2; i++) {
            for (int j = 0; j < board.length - 2; j++) {
                int line = checkLines(board, 3, i, j, board.length == 3);
                if (Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE).contains(line)) {
                    return line;
                } else {
                    sum += line;
                }
            }
        }

        if (listPossibleCommands().isEmpty()) {
            return null;
        } else {
            return sum;
        }
    }

    @Override
    public String getInstructions() {
        return "Pick the spot you want. \"1,3\" is line 1, column 3.";
    }

    static int checkLines(Boolean[][] board, int length, int startI, int startJ, boolean diagonals) {
        int score = 0;

        for (int beginI = startI; beginI < startI + length; beginI++) {
            // check all lines
            int countTrue = 0;
            int countFalse = 0;
            for (int j = startJ; j < startJ + length; j++) {
                if (board[beginI][j] == Boolean.TRUE) countTrue++;
                if (board[beginI][j] == Boolean.FALSE) countFalse++;
            }

            if (countTrue == length) return Integer.MAX_VALUE;
            if (countFalse == length) return Integer.MIN_VALUE;

            if (countFalse + countTrue > 1 && (countFalse == 0) != (countTrue == 0)) {
                score += (Math.pow(10, countTrue) - Math.pow(10, countFalse));
            }
        }

        for (int beginJ = startJ; beginJ < startJ + length; beginJ++) {
            // check all columns
            int countTrue = 0;
            int countFalse = 0;
            for (int i = startI; i < startI + length; i++) {
                if (board[i][beginJ] == Boolean.TRUE) countTrue++;
                if (board[i][beginJ] == Boolean.FALSE) countFalse++;
            }

            if (countTrue == length) return Integer.MAX_VALUE;
            if (countFalse == length) return Integer.MIN_VALUE;

            if (countFalse + countTrue > 1 && (countFalse == 0) != (countTrue == 0)) {
                score += (Math.pow(10, countTrue) - Math.pow(10, countFalse));
            }
        }

        if (diagonals) {
            {
                // check the top-left > bottom-right diagonal
                int countTrue = 0;
                int countFalse = 0;

                for (int k = 0; k < length; k++) {
                    if (board[startI + k][startJ + k] == Boolean.TRUE) countTrue++;
                    if (board[startI + k][startJ + k] == Boolean.FALSE) countFalse++;
                }

                if (countTrue == length) return Integer.MAX_VALUE;
                if (countFalse == length) return Integer.MIN_VALUE;

                if (countFalse + countTrue > 1 && (countFalse == 0) != (countTrue == 0)) {
                    score += (Math.pow(10, countTrue) - Math.pow(10, countFalse));
                }
            }

            {
                // check the bottom-left > top-right diagonal
                int countTrue = 0;
                int countFalse = 0;

                for (int k = 0; k < length; k++) {
                    if (board[startI + length - 1 - k][startJ + k] == Boolean.TRUE) countTrue++;
                    if (board[startI + length - 1 - k][startJ + k] == Boolean.FALSE) countFalse++;
                }

                if (countTrue == length) return Integer.MAX_VALUE;
                if (countFalse == length) return Integer.MIN_VALUE;

                if (countFalse + countTrue > 1 && (countFalse == 0) != (countTrue == 0)) {
                    score += (Math.pow(10, countTrue) - Math.pow(10, countFalse));
                }
            }
        }

        return score;
    }

    public static TicTacToe generateRandom() {
        TicTacToe rng = new TicTacToe(Math.random() > 0.5, (int) (Math.random() * 2) + 3);
        for (int i = 0; i < rng.board.length; i++) {
            for (int j = 0; j < rng.board[0].length; j++) {
                double random = Math.random() * 3;
                if (random > 2) {
                    rng.board[i][j] = true;
                } else if (random > 1) {
                    rng.board[i][j] = false;
                }
            }
        }
        return rng;
    }
}
