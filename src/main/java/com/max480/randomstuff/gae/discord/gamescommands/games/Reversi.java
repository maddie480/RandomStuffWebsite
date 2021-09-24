package com.max480.randomstuff.gae.discord.gamescommands.games;


import com.max480.randomstuff.gae.discord.gamescommands.status.GameState;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Reversi extends GameState {

    // Black = -1, White = 1, unless this is true
    private boolean invertedColors;

    // 1 = Player 1, -1 = Player 2
    private int[][] board;

    public Reversi(boolean player1Starts) {
        super(player1Starts);

        // If Player 1 starts, Black = 1. Otherwise, Black = -1
        invertedColors = player1Starts;

        int invert = player1Starts ? 1 : -1;

        board = new int[8][8];
        board[3][3] = invert;
        board[4][4] = invert;
        board[3][4] = -invert;
        board[4][3] = -invert;
    }

    @Override
    protected void serializeGame(ObjectOutputStream os) throws IOException {
        os.writeBoolean(invertedColors);
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j += 4) {
                GameState.writeBooleans(os, new boolean[]{
                        board[i][j] == 0, board[i][j] == 1,
                        board[i][j + 1] == 0, board[i][j + 1] == 1,
                        board[i][j + 2] == 0, board[i][j + 2] == 1,
                        board[i][j + 3] == 0, board[i][j + 3] == 1
                });
            }
        }
    }

    public Reversi(ObjectInputStream is) throws IOException {
        super(is);

        invertedColors = is.readBoolean();

        board = new int[8][8];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j += 4) {
                boolean[] chunk = new boolean[8];
                GameState.readBooleans(is, chunk);

                for (int k = 0; k < 4; k++) {
                    boolean is0 = chunk[k * 2];
                    boolean is1 = chunk[k * 2 + 1];
                    board[i][j + k] = is0 ? 0 : (is1 ? 1 : -1);
                }
            }
        }
    }

    @Override
    public String getInstructions() {
        if (possibleMoves(getCurrentPlayer()).isEmpty()) {
            return "You cannot play! Hit \"skip\" to skip your turn.";
        } else {
            return "Pick the spot you want. \"A3\" is line A, column 3.";
        }
    }

    private List<Point> getCoinsToFlip(int player, int x, int y) {
        List<Point> coinsToFlip = new ArrayList<>();

        for (int dirX = -1; dirX <= 1; dirX++) {
            for (int dirY = -1; dirY <= 1; dirY++) {
                if (dirX == 0 && dirY == 0) continue;

                List<Point> crossedSpots = new ArrayList<>();

                int pX = x + dirX;
                int pY = y + dirY;

                while (pX >= 0 && pX < 8 && pY >= 0 && pY < 8 && board[pX][pY] == -player) {
                    // we're still on the board, and on enemy spots => continue
                    crossedSpots.add(new Point(pX, pY));

                    pX += dirX;
                    pY += dirY;
                }

                if (pX >= 0 && pX < 8 && pY >= 0 && pY < 8 && board[pX][pY] == player) {
                    // we stopped on one of our own pieces, and every spot up to here
                    // were enemy pieces => gotta catch them all
                    coinsToFlip.addAll(crossedSpots);
                }
            }
        }

        return coinsToFlip;
    }

    private List<Point> possibleMoves(int player) {
        List<Point> possibleMoves = new ArrayList<>();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (board[x][y] == 0 && !getCoinsToFlip(player, x, y).isEmpty()) {
                    // the spot is empty, and I can capture pieces if I play here => I can play here
                    possibleMoves.add(new Point(x, y));
                }
            }
        }

        return possibleMoves;
    }

    @Override
    public List<String> listPossibleCommands() {
        List<String> possibleMoves = possibleMoves(getCurrentPlayer()).stream()
                .map(point -> (char) ('A' + point.y) + "" + (point.x + 1))
                .collect(Collectors.toList());

        if (possibleMoves.isEmpty()) {
            return Collections.singletonList("skip");
        } else {
            Collections.shuffle(possibleMoves);
            return possibleMoves;
        }
    }

    @Override
    public GameState applyCommand(String command) {
        Reversi status = copyStatus(command);

        if (!command.toLowerCase().equals("skip")) {
            command = command.toUpperCase();
            int y = command.charAt(0) - 'A';
            int x = command.charAt(1) - '1'; // this is how you parse a number

            for (Point p : status.getCoinsToFlip(status.getCurrentPlayer(), x, y)) {
                status.board[p.x][p.y] = -status.board[p.x][p.y];
            }
            status.board[x][y] = status.getCurrentPlayer();
        }

        status.switchPlayers();
        return status;
    }

    @Override
    protected GameState copy() {
        Reversi newStatus = new Reversi(isPlayer1Turn());

        newStatus.invertedColors = invertedColors;
        for (int x = 0; x < 8; x++) {
            System.arraycopy(board[x], 0, newStatus.board[x], 0, 8);
        }

        return newStatus;
    }

    @Override
    public String displayStatus() {
        Set<Point> possibleMoves = new HashSet<>(possibleMoves(getCurrentPlayer()));

        int countWhite = 0;
        int countBlack = 0;

        StringBuilder string = new StringBuilder("```\n.| 1 2 3 4 5 6 7 8 |\n |-----------------|\n");
        for (int y = 0; y < 8; y++) {
            string.append((char) ('A' + y)).append('|');
            for (int x = 0; x < 8; x++) {
                string.append(' ');
                switch (board[x][y] * (invertedColors ? -1 : 1)) {
                    default:
                        string.append('?');
                        break;
                    case 1:
                        string.append('●');
                        countWhite++;
                        break;
                    case -1:
                        string.append('○');
                        countBlack++;
                        break;
                    case 0:
                        if (possibleMoves.contains(new Point(x, y))) {
                            string.append('-');
                        } else {
                            string.append(' ');
                        }
                        break;
                }
            }
            string.append(" |\n");
        }

        string.append(" |-----------------|\n● = ").append(countWhite).append(", ○ = ").append(countBlack).append("\n```");
        return string.toString();
    }

    private int getCurrentPlayer() {
        return isPlayer1Turn() ? 1 : -1;
    }

    @Override
    public Integer scoreSituation() {
        // count the points
        int j1 = 0, j2 = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (board[x][y] == -1) j2++;
                else if (board[x][y] == 1) j1++;
            }
        }

        int countPossibleMovesP1 = possibleMoves(1).size();
        int countPossibleMovesP2 = possibleMoves(-1).size();

        // let's check if the game is over first: it is if none of the 2 players can play
        if (countPossibleMovesP1 == 0 && countPossibleMovesP2 == 0) {
            if (j2 > j1) return Integer.MIN_VALUE;
            else if (j1 > j2) return Integer.MAX_VALUE;
            else return null;
        }

        // heuristic 1: piece parity
        double coinParity = (double) (j1 - j2) / (j1 + j2) * 100.;

        // heuristic 2: mobility
        double mobility = (double) (countPossibleMovesP1 - countPossibleMovesP2) / (countPossibleMovesP1 + countPossibleMovesP2) * 100;

        // heuristic 3: corners
        int nbCoinsJ1 = 0;
        int nbCoinsJ2 = 0;

        for (int i = 0; i < 8; i += 7) {
            for (int j = 0; j < 8; j += 7) {
                switch (board[i][j]) {
                    case 1:
                        nbCoinsJ1++;
                        break;
                    case -1:
                        nbCoinsJ2++;
                        break;
                }
            }
        }

        double corners = 0;
        if (nbCoinsJ1 + nbCoinsJ2 != 0) {
            corners = (double) (nbCoinsJ1 - nbCoinsJ2) / (nbCoinsJ1 + nbCoinsJ2) * 100;
        }

        return (int) ((coinParity + mobility + corners) * 1_000_000);
    }

    public static Reversi generateRandom() {
        Reversi rng = new Reversi(Math.random() > 0.5);
        rng.invertedColors = Math.random() > 0.5;
        for (int i = 0; i < rng.board.length; i++) {
            for (int j = 0; j < rng.board[0].length; j++) {
                rng.board[i][j] = (int) (Math.random() * 3) - 1;
            }
        }
        return rng;
    }
}
