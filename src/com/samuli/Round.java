package com.samuli;

import java.util.ArrayList;


public class Round {
    public final ArrayList<Game> games;
    private ErrorCalculator errorCalculator;
    public final ArrayList<Game> boundGames;


    public Round() {
        games = new ArrayList<>();
        boundGames = new ArrayList<>();

        errorCalculator = new ErrorCalculator(games);
    }

    public Round clone() {
        Round output = new Round();
        output.errorCalculator = errorCalculator.clone(output.games);
        // Clone the games
        for (Game g : games) {
            output.addGame(g);
        }
        for (Game g : boundGames) {
            output.addBoundGame(g);
        }
        return output;
    }

    public ArrayList<Game> getBoundGames() {
        // Copy it into a new array, so cannot accidentally change it outside this class
        ArrayList<Game> output = new ArrayList<>();
        output.addAll(boundGames);
        return output;
    }

    public ArrayList<Game> getGames() {
        // Copy it into a new array, so cannot accidentally change it outside this class
        ArrayList<Game> output = new ArrayList<>();
        output.addAll(games);
        return output;
    }

    public void addGame(Game game) {
        games.add(game);
        errorCalculator.addGame(game);
    }

    public void setAwayGameLimit(Team team) {
        errorCalculator.setAwayGameLimit(team);
    }

    public void setHomeGameLimit(Team team) {
        errorCalculator.setHomeGameLimit(team);
    }

    public void addBoundGame(Game game) {
        boundGames.add(game);
        errorCalculator.addGame(game);
    }

    // This should only be called from Population.removeGame!
    public void removeGame(Game game) {
        if (games.remove(game) == false) {
            // If the game didn't exist
            return;
        }
        errorCalculator.removeGame(game);
    }

    public Game getRandomGame() {
        return games.get(Globals.randomGen.nextInt(games.size()));
    }

    public GameRoundPair getHighestErrorGame(TabuList tabuList) {
        if (games.size() == 0) {
            // No game to choose from. Return null and skip this
            return null;
        }
        // The index in games Array will be the game's unique id for this and chooseGameFromErrorArray function
        // All the error classes use a reference to this same games list, so the order they use will be same as well
        int[] errorsByGame = new int[games.size()];
        // Calculate the errors for games
        for (int i = 0; i < games.size(); i++) {
            Game g = games.get(i);
            errorsByGame[i] = 0;

            errorsByGame[i] += errorCalculator.getTeamCountsErrorByGame(g) * Constants.GAME_COUNT_ERROR * Constants.HARD_ERROR;
            // Away and home game limit errors
            errorsByGame[i] += errorCalculator.getAwayErrorByTeam(games.get(i).guest) * Constants.AWAY_GAME_ERROR * Constants.HARD_ERROR;
            errorsByGame[i] += errorCalculator.getHomeErrorByTeam(games.get(i).home) * Constants.HOME_GAME_ERROR * Constants.HARD_ERROR;

            // Use tabulist here. Change the not allowed game's error to negative, so it will never be chosen
            if (tabuList.isInList(this, games.get(i))) {
                errorsByGame[i] = -5;
                continue;
            }
        }
        return chooseGameFromErrorArray(errorsByGame);
    }

    public int getTotalErrorsWithMods() {
        int error = 0;
        error += errorCalculator.getTotalTeamCountErrors() * Constants.GAME_COUNT_ERROR * Constants.HARD_ERROR;
        error += errorCalculator.getAwayErrors() * Constants.AWAY_GAME_ERROR * Constants.HARD_ERROR;
        error += errorCalculator.getHomeErrors() * Constants.HOME_GAME_ERROR * Constants.HARD_ERROR;
        return error;
    }

    public int getTeamCountError() {
        return errorCalculator.getTotalTeamCountErrors();
    }

    public int getAwayErrors() {
        return errorCalculator.getAwayErrors();
    }

    public int getHomeErrors() {
        return errorCalculator.getHomeErrors();
    }

    public int getHardErrors() {
        int error = 0;
        error += getTeamCountError();
        error += errorCalculator.getAwayErrors();
        error += errorCalculator.getHomeErrors();
        return error;
    }

    public int getSoftErrors() {
        int total = 0;
        return total;
    }

    private GameRoundPair chooseGameFromErrorArray(int[] errorsByGame) {
        // Find highest error
        int highest = 0;
        for (int anErrorsByGame : errorsByGame) {
            if (anErrorsByGame > highest) {
                highest = anErrorsByGame;
            }
        }

        // Find indexes with the highest error
        ArrayList<Integer> highestGames = new ArrayList<>();
        for (int i = 0; i < errorsByGame.length; i++) {
            if (errorsByGame[i] == highest) highestGames.add(i);
        }

        if (highestGames.size() == 0) {
            // Nothing to choose from, meaning all games are in tabulist. Return null so it will just skip
            if (highest == 0) {
                return null;
            }
            // Should never get here!
            System.out.println("highestGames.size() == 0 " + errorsByGame);
            System.out.println("Game count " + games.size());
            // If games.size() is 0, next line throws and exception. There is a check earlier. If this happens still change the fix
        }
        // Choose one of them
        Integer chosenId = highestGames.size() == 1 ? highestGames.get(0) : highestGames.get(Globals.randomGen.nextInt(highestGames.size()));
        Game chosenGame = games.get(chosenId);

        return new GameRoundPair(
                chosenGame,
                this,
                highest);
    }

    public String description() {
        StringBuilder sb = new StringBuilder();
        sb.append("Total games: " + games.size() + ", Total error: " + getTotalErrorsWithMods() + "\n");
        return sb.toString();
    }
}
