package com.max480.randomstuff.gae.discord.gamescommands.games;


import com.max480.randomstuff.gae.discord.gamescommands.status.GameState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Connect4 extends GameState {

    private final Boolean[][] columns = new Boolean[7][6];

    public Connect4(boolean player1Starts) {
        super(player1Starts);
    }

    @Override
    protected void serializeGame(ObjectOutputStream os) throws IOException {
        for (int i = 0; i < columns.length; i++) {
            GameState.writeBooleans(os, new boolean[]{
                    columns[i][0] == null, columns[i][0] == Boolean.TRUE,
                    columns[i][1] == null, columns[i][1] == Boolean.TRUE,
                    columns[i][2] == null, columns[i][2] == Boolean.TRUE,
                    columns[i][3] == null, columns[i][3] == Boolean.TRUE
            });
            GameState.writeBooleans(os, new boolean[]{
                    columns[i][4] == null, columns[i][4] == Boolean.TRUE,
                    columns[i][5] == null, columns[i][5] == Boolean.TRUE
            });
        }
    }

    public Connect4(ObjectInputStream is) throws IOException {
        super(is);

        for (int i = 0; i < columns.length; i++) {
            boolean[] chunk1 = new boolean[8];
            boolean[] chunk2 = new boolean[4];
            GameState.readBooleans(is, chunk1);
            GameState.readBooleans(is, chunk2);

            for (int j = 0; j < chunk1.length / 2; j++) {
                boolean isNull = chunk1[j * 2];
                boolean isTrue = chunk1[j * 2 + 1];
                columns[i][j] = isNull ? null : isTrue;
            }
            for (int j = 0; j < chunk2.length / 2; j++) {
                boolean isNull = chunk2[j * 2];
                boolean isTrue = chunk2[j * 2 + 1];
                columns[i][j + chunk1.length / 2] = isNull ? null : isTrue;
            }
        }
    }

    @Override
    public String getInstructions() {
        return "Pick the number of the column you want.";
    }

    @Override
    public List<String> listPossibleCommands() {
        List<String> commands = new ArrayList<>(Arrays.asList("4", "3", "5", "2", "6", "1", "7"));

        for (int i = 0; i < 7; i++) {
            if (columns[i][5] != null) {
                commands.remove((i + 1) + "");
            }
        }

        return commands;
    }

    @Override
    public GameState applyCommand(String command) {
        int i = Integer.parseInt(command) - 1;

        int j = 0;
        while (columns[i][j] != null) {
            j++;
        }

        Connect4 newStatus = copyStatus(command);
        newStatus.columns[i][j] = isPlayer1Turn();
        newStatus.switchPlayers();

        return newStatus;
    }

    @Override
    protected GameState copy() {
        Connect4 status = new Connect4(true);

        // copy the board
        for (int i = 0; i < columns.length; i++) {
            System.arraycopy(this.columns[i], 0, status.columns[i], 0, columns[0].length);
        }
        return status;
    }

    @Override
    public String displayStatus() {
        StringBuilder stringBuilder = new StringBuilder("```\n|");
        for (int i = 1; i <= 7; i++) {
            stringBuilder.append(" ").append(i);
        }
        stringBuilder.append(" |\n");
        for (int j = 5; j >= 0; j--) {
            stringBuilder.append("|");
            for (int i = 0; i < 7; i++) {
                if (columns[i][j] == Boolean.TRUE) stringBuilder.append(" ●");
                else if (columns[i][j] == Boolean.FALSE) stringBuilder.append(" ○");
                else stringBuilder.append("  ");
            }
            stringBuilder.append(" |\n");
        }
        return stringBuilder.append("```").toString();
    }

    @Override
    public Integer scoreSituation() {
        int somme = 0;
        for (int i = 0; i < columns.length - 3; i++) {
            for (int j = 0; j < columns[0].length - 3; j++) {
                int line = TicTacToe.checkLines(columns, 4, i, j, true);
                if (Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE).contains(line)) {
                    return line;
                } else {
                    somme += line;
                }
            }
        }

        if (listPossibleCommands().isEmpty()) {
            return null;
        } else {
            return somme;
        }
    }

    public static Connect4 generateRandom() {
        Connect4 rng = new Connect4(Math.random() > 0.5);
        for (int i = 0; i < rng.columns.length; i++) {
            for (int j = 0; j < rng.columns[0].length; j++) {
                double random = Math.random() * 3;
                if (random > 2) {
                    rng.columns[i][j] = true;
                } else if (random > 1) {
                    rng.columns[i][j] = false;
                }
            }
        }
        return rng;
    }
}
