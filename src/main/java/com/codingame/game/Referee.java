package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;

import org.yaml.snakeyaml.scanner.Constant;

import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {
    // Uncomment the line below and comment the line under it to create a Solo Game
    // @Inject private SoloGameManager<Player> gameManager;
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;

    private int currentPlayer = 0;
    private Board board = null;
    private static final int WINNING_SCORE = 2000;

    @Override
    public void init() {
        // Initialize your game here.
    }

    @Override
    public void gameTurn(int turn) {
        Player player = gameManager.getPlayer(currentPlayer);
        if (board == null) {
            board = new Board(6);
        }

        System.out.println(String.format("Next board |%s|",board.toString()));
        gameManager.addToGameSummary(
            String.format("Board |%s| for %s", board.toString(), player.getNicknameToken())
        );

        // Check the score
        if (board.getScore() == 0) {
            // Player lose
            gameManager.addToGameSummary(
                GameManager.formatErrorMessage(
                    String.format("%s lose %d points with board |%s|", player.getNicknameToken(), board.getTotalScore(), board.toString())
                )
            );
            currentPlayer = currentPlayer == 0 ? 1 : 0;
            board = new Board(6);
        }
        // Check if turn is over
        else if (board.turnIsOver()) {
            player.setScore(player.getScore() + board.getTotalScore());
            gameManager.addToGameSummary(
                String.format("%s ends his turn with %d points and a total of %d points", player.getNicknameToken(), board.getTotalScore(), player.getScore())
            );
            // Check for winner
            if (player.getScore() > WINNING_SCORE) {
                gameManager.addToGameSummary(GameManager.formatSuccessMessage(player.getNicknameToken() + " won!"));
                gameManager.endGame();
            } else {
                currentPlayer = currentPlayer == 0 ? 1 : 0;
                board = new Board(6);
            }
        }

        else {
            player.sendInputLine(board.toString());
            player.execute();
            try {
                // Check validity of the player output and compute the new game state
                String output = player.getOutputs().get(0);
                if (output.equals("pass")) {
                    player.setScore(player.getScore() + board.getTotalScore());
                    gameManager.addToGameSummary(
                        String.format("%s passes with %d points and a total of %d points", player.getNicknameToken(), board.getTotalScore(), player.getScore())
                    );
                    // Check for winner
                    if (player.getScore() > WINNING_SCORE) {
                        gameManager.addToGameSummary(GameManager.formatSuccessMessage(player.getNicknameToken() + " won!"));
                        gameManager.endGame();
                    } else {
                        currentPlayer = currentPlayer == 0 ? 1 : 0;
                        board = new Board(6);
                    }
                } else if (!board.applyChoice(output)) {
                    throw new InvalidBoard("Invalid board.");
                } else {
                    gameManager.addToGameSummary(
                        String.format("%s keeps |%s|", player.getNicknameToken(), output)
                    );
                }

            } catch (TimeoutException e) {
                gameManager.addToGameSummary(GameManager.formatErrorMessage(String.format("$%d timeout!", player.getIndex())));
                player.deactivate(String.format("$%d timeout!", player.getIndex()));
            } catch (InvalidBoard e) {
                gameManager.addToGameSummary(GameManager.formatErrorMessage(player.getNicknameToken() + " lose!"));
                gameManager.addToGameSummary(GameManager.formatErrorMessage(board.toString()));
                player.deactivate(e.getMessage());
                player.setScore(-1);
                gameManager.endGame();
            }
        }
    }
}
