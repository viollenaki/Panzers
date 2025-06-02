package com.server.Panzers.dto;

import java.util.List;
import java.util.Map;

import com.server.Panzers.model.game.Bullet;
import com.server.Panzers.model.game.Tank;

public class GameStateDTO {

    private String type = "GAME_STATE_UPDATE";
    private long timestamp;
    private List<Tank> tanks;
    private List<Bullet> bullets;
    private Map<String, Integer> scores;
    private Map<String, PlayerStats> playerStats;
    private GameInfo gameInfo;

    public static class PlayerStats {

        private String playerName;
        private int kills;
        private int deaths;
        private int health;
        private int ammunition;
        private boolean isAlive;

        // Constructors
        public PlayerStats() {
        }

        public PlayerStats(String playerName, int kills, int deaths, int health, int ammunition, boolean isAlive) {
            this.playerName = playerName;
            this.kills = kills;
            this.deaths = deaths;
            this.health = health;
            this.ammunition = ammunition;
            this.isAlive = isAlive;
        }

        // Getters and Setters
        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public int getKills() {
            return kills;
        }

        public void setKills(int kills) {
            this.kills = kills;
        }

        public int getDeaths() {
            return deaths;
        }

        public void setDeaths(int deaths) {
            this.deaths = deaths;
        }

        public int getHealth() {
            return health;
        }

        public void setHealth(int health) {
            this.health = health;
        }

        public int getAmmunition() {
            return ammunition;
        }

        public void setAmmunition(int ammunition) {
            this.ammunition = ammunition;
        }

        public boolean isAlive() {
            return isAlive;
        }

        public void setAlive(boolean alive) {
            isAlive = alive;
        }

        public void setMoving(boolean moving) {
            // This can be used for future functionality if needed
        }
    }

    public static class GameInfo {

        private int activePlayers;
        private long gameStartTime;
        private long gameDuration;
        private String gameStatus;

        // Constructors
        public GameInfo() {
        }

        public GameInfo(int activePlayers, long gameStartTime, String gameStatus) {
            this.activePlayers = activePlayers;
            this.gameStartTime = gameStartTime;
            this.gameStatus = gameStatus;
            this.gameDuration = System.currentTimeMillis() - gameStartTime;
        }

        // Getters and Setters
        public int getActivePlayers() {
            return activePlayers;
        }

        public void setActivePlayers(int activePlayers) {
            this.activePlayers = activePlayers;
        }

        public long getGameStartTime() {
            return gameStartTime;
        }

        public void setGameStartTime(long gameStartTime) {
            this.gameStartTime = gameStartTime;
        }

        public long getGameDuration() {
            return gameDuration;
        }

        public void setGameDuration(long gameDuration) {
            this.gameDuration = gameDuration;
        }

        public String getGameStatus() {
            return gameStatus;
        }

        public void setGameStatus(String gameStatus) {
            this.gameStatus = gameStatus;
        }
    }

    // Constructors
    public GameStateDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public GameStateDTO(List<Tank> tanks, List<Bullet> bullets, Map<String, Integer> scores,
            Map<String, PlayerStats> playerStats, GameInfo gameInfo) {
        this();
        this.tanks = tanks;
        this.bullets = bullets;
        this.scores = scores;
        this.playerStats = playerStats;
        this.gameInfo = gameInfo;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Tank> getTanks() {
        return tanks;
    }

    public void setTanks(List<Tank> tanks) {
        this.tanks = tanks;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public void setBullets(List<Bullet> bullets) {
        this.bullets = bullets;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }

    public Map<String, PlayerStats> getPlayerStats() {
        return playerStats;
    }

    public void setPlayerStats(Map<String, PlayerStats> playerStats) {
        this.playerStats = playerStats;
    }

    public GameInfo getGameInfo() {
        return gameInfo;
    }

    public void setGameInfo(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }
}
