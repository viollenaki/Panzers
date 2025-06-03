package com.server.Panzers.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.Panzers.dto.GameStateDTO;
import com.server.Panzers.dto.PlayerActionDTO;
import com.server.Panzers.model.GameSession;
import com.server.Panzers.model.User;
import com.server.Panzers.model.game.Bullet;
import com.server.Panzers.model.game.Tank;
import com.server.Panzers.model.game.Tank.Direction;

@Service
public class GameService {

    private static final Logger LOGGER = Logger.getLogger(GameService.class.getName());
    private static final Random RANDOM = new Random();

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final StatisticsService statisticsService;
    private final GameSessionService gameSessionService;

    public GameService(SimpMessagingTemplate messagingTemplate, UserService userService,
            StatisticsService statisticsService, GameSessionService gameSessionService) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.statisticsService = statisticsService;
        this.gameSessionService = gameSessionService;
    }

    // Game state storage
    private final Map<String, Tank> activeTanks = new ConcurrentHashMap<>();
    private final List<Bullet> activeBullets = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();
    private final Map<String, GameStateDTO.PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Map<String, String> playerNames = new ConcurrentHashMap<>();
    private final Map<String, GameSession> playerSessions = new ConcurrentHashMap<>(); // Track active sessions

    // Game constants
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    public static final int WALL_SIZE = 40;

    // Spawn points for new players
    private final double[][] spawnPoints = {
        {100, 100}, {700, 100}, {100, 500}, {700, 500},
        {400, 300}, {200, 300}, {600, 300}, {400, 150}
    };

    private final long gameStartTime = System.currentTimeMillis();

    public void handlePlayerAction(PlayerActionDTO action) {
        String actionType = action.getType();
        String playerId = action.getPlayerId();
        PlayerActionDTO.ActionData data = action.getData();

        switch (actionType) {
            case "PLAYER_JOIN" ->
                handlePlayerJoin(playerId, data);
            case "PLAYER_MOVE" ->
                handlePlayerMove(playerId, data);
            case "PLAYER_SHOOT" ->
                handlePlayerShoot(playerId);
            case "PLAYER_STOP" ->
                handlePlayerStop(playerId);
            case "PLAYER_RELOAD" ->
                handlePlayerReload(playerId);
            case "PLAYER_LEAVE" ->
                handlePlayerLeave(playerId);
            default ->
                LOGGER.warning("Unknown action type: " + actionType);
        }
    }

    private void handlePlayerJoin(String playerId, PlayerActionDTO.ActionData data) {
        if (activeTanks.containsKey(playerId)) {
            return; // Player already in game
        }

        // Find available spawn point
        double[] spawnPoint = findAvailableSpawnPoint();

        // Create new tank
        Tank tank = new Tank(playerId, spawnPoint[0], spawnPoint[1], generatePlayerColor());
        activeTanks.put(playerId, tank);

        // Initialize player stats
        String playerName = (data != null && data.getPlayerName() != null)
                ? data.getPlayerName()
                : "Player" + playerId.substring(Math.max(0, playerId.length() - 6));
        playerNames.put(playerId, playerName);
        playerScores.put(playerId, 0);
        playerStats.put(playerId, new GameStateDTO.PlayerStats(
                playerNames.get(playerId), 0, 0, Tank.MAX_HEALTH, Tank.MAX_AMMUNITION, true
        ));

        // Create game session for registered users
        createGameSession(playerId, playerName);

        broadcastGameState();
    }

    private void createGameSession(String playerId, String playerName) {
        try {
            // Try to find user by username (if logged in)
            User user = userService.findByUsername(playerName);
            if (user != null) {
                GameSession session = gameSessionService.createSession(user);
                playerSessions.put(playerId, session);
                LOGGER.info("Created game session for user: " + playerName);
            } else {
                LOGGER.info("Anonymous player joined: " + playerName);
            }
        } catch (Exception e) {
            LOGGER.warning("Error creating game session for player " + playerName + ": " + e.getMessage());
        }
    }

    private void handlePlayerMove(String playerId, PlayerActionDTO.ActionData data) {
        Tank tank = activeTanks.get(playerId);
        if (tank == null || !tank.isAlive()) {
            return;
        }

        // Validate movement data
        if (data.getDirection() == null) {
            return;
        }

        double newX = data.getX();
        double newY = data.getY();
        Direction direction = data.getDirection();
        boolean isMoving = data.isMoving();
        double angle = data.getAngle(); // Get angle from client

        // Server-side boundary validation
        double halfSize = Tank.TANK_SIZE / 2.0;
        newX = Math.max(halfSize, Math.min(GAME_WIDTH - halfSize, newX));
        newY = Math.max(halfSize, Math.min(GAME_HEIGHT - halfSize, newY));

        // Check for collisions with other tanks
        boolean collisionDetected = false;
        for (Tank otherTank : activeTanks.values()) {
            if (!otherTank.getId().equals(tank.getId())
                    && otherTank.isAlive()
                    && otherTank.intersects(newX, newY, Tank.TANK_SIZE)) {
                collisionDetected = true;
                break;
            }
        }

        if (!collisionDetected) {
            tank.setX(newX);
            tank.setY(newY);
            tank.setDirection(direction);
            tank.setMoving(isMoving);

            // Update angle if provided
            if (angle != 0) {
                tank.setAngle(angle);
                tank.updateDirectionFromAngle();
            }
        } else {
            // Stop tank if collision detected
            tank.setMoving(false);
        }
    }

    private void handlePlayerShoot(String playerId) {
        Tank tank = activeTanks.get(playerId);
        if (tank == null || !tank.isAlive()) {
            return;
        }

        Bullet bullet = tank.shoot();
        if (bullet != null) {
            activeBullets.add(bullet);

            // Update player stats
            GameStateDTO.PlayerStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.setAmmunition(tank.getAmmunition());
            }

            // Update session statistics
            updateSessionShot(playerId);
        }
    }

    private void updateSessionShot(String playerId) {
        GameSession session = playerSessions.get(playerId);
        if (session != null) {
            try {
                gameSessionService.recordShot(session);
            } catch (Exception e) {
                LOGGER.warning("Error updating shot stats for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    private void handlePlayerStop(String playerId) {
        Tank tank = activeTanks.get(playerId);
        if (tank != null) {
            tank.stop();
        }
    }

    private void handlePlayerReload(String playerId) {
        Tank tank = activeTanks.get(playerId);
        if (tank == null || !tank.isAlive()) {
            return;
        }

        if (tank.getAmmunition() < Tank.MAX_AMMUNITION) {
            tank.reload();
            GameStateDTO.PlayerStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.setAmmunition(tank.getAmmunition());
            }
            broadcastGameState();
        }
    }

    @Scheduled(fixedRate = 16) // ~60 FPS
    public void gameLoop() {
        updateBullets();
        checkCollisions();
        broadcastGameState();
    }

    private void updateBullets() {
        Iterator<Bullet> bulletIterator = activeBullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update();

            if (!bullet.isActive() || isOutOfBounds(bullet.getX(), bullet.getY())) {
                bulletIterator.remove();
            }
        }
    }

    private void checkCollisions() {
        processCollisions();
    }

    private void processCollisions() {
        Iterator<Bullet> bulletIterator = activeBullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            if (processBulletCollisions(bullet)) {
                bulletIterator.remove();
            }
        }
    }

    private boolean processBulletCollisions(Bullet bullet) {
        // Check bullet vs tank collisions
        for (Tank tank : activeTanks.values()) {
            if (bullet.intersects(tank) && tank.isAlive()) {
                handleBulletHit(bullet, tank);
                bullet.destroy();
                return true;
            }
        }

        // Check bullet vs wall collisions
        if (bullet.getX() <= 0 || bullet.getX() >= GAME_WIDTH
                || bullet.getY() <= 0 || bullet.getY() >= GAME_HEIGHT) {
            bullet.destroy();
            return true;
        }
        return false;
    }

    private void handleBulletHit(Bullet bullet, Tank tank) {
        tank.takeDamage(bullet.getDamage());

        GameStateDTO.PlayerStats targetStats = playerStats.get(tank.getPlayerId());
        if (targetStats != null) {
            targetStats.setHealth(tank.getHealth());

            if (!tank.isAlive()) {
                // Handle tank death
                targetStats.setAlive(false);
                targetStats.setDeaths(targetStats.getDeaths() + 1);

                // Record death in session
                updateSessionDeath(tank.getPlayerId());

                // Handle killer stats
                GameStateDTO.PlayerStats shooterStats = playerStats.get(bullet.getOwnerId());
                if (shooterStats != null) {
                    shooterStats.setKills(shooterStats.getKills() + 1);
                    int scoreGain = 100;
                    playerScores.put(bullet.getOwnerId(),
                            playerScores.getOrDefault(bullet.getOwnerId(), 0) + scoreGain);

                    // Record kill and hit in session
                    updateSessionKill(bullet.getOwnerId(), scoreGain);
                }

                respawnTank(tank);
            } else {
                // Just a hit, not a kill
                updateSessionHit(bullet.getOwnerId());
            }
        }
    }

    private void updateSessionDeath(String playerId) {
        GameSession session = playerSessions.get(playerId);
        if (session != null) {
            try {
                gameSessionService.recordDeath(session);
            } catch (Exception e) {
                LOGGER.warning("Error updating death stats for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    private void updateSessionKill(String playerId, int scoreGain) {
        GameSession session = playerSessions.get(playerId);
        if (session != null) {
            try {
                gameSessionService.recordKill(session, scoreGain);
                gameSessionService.recordHit(session); // Kill counts as hit too
            } catch (Exception e) {
                LOGGER.warning("Error updating kill stats for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    private void updateSessionHit(String playerId) {
        GameSession session = playerSessions.get(playerId);
        if (session != null) {
            try {
                gameSessionService.recordHit(session);
            } catch (Exception e) {
                LOGGER.warning("Error updating hit stats for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    private void respawnTank(Tank tank) {
        // Wait a bit before respawning
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Find a new spawn point
                double[] spawnPoint = findAvailableSpawnPoint();
                
                // Reset tank position and status
                tank.setX(spawnPoint[0]);
                tank.setY(spawnPoint[1]);
                tank.setHealth(Tank.MAX_HEALTH);
                tank.setAmmunition(Tank.MAX_AMMUNITION);
                
                // Update player stats
                GameStateDTO.PlayerStats stats = playerStats.get(tank.getPlayerId());
                if (stats != null) {
                    stats.setAlive(true);
                    stats.setHealth(tank.getHealth());
                    stats.setAmmunition(tank.getAmmunition());
                }
                
                broadcastGameState();
            }
        }, 3000);  // 3 seconds respawn delay
    }

    private void handlePlayerLeave(String playerId) {
        // End game session before removing player
        endGameSession(playerId);

        activeTanks.remove(playerId);
        playerStats.remove(playerId);
        playerScores.remove(playerId);
        playerNames.remove(playerId);
        playerSessions.remove(playerId);

        broadcastGameState();
    }

    private void endGameSession(String playerId) {
        GameSession session = playerSessions.get(playerId);
        if (session != null) {
            try {
                Integer finalScore = playerScores.get(playerId);
                gameSessionService.endSession(session, finalScore != null ? finalScore : 0);
                LOGGER.info("Ended game session for player: " + playerId);
            } catch (Exception e) {
                LOGGER.warning("Error ending game session for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    // Scheduled method to save periodic updates
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void savePeriodicUpdates() {
        for (Map.Entry<String, GameSession> entry : playerSessions.entrySet()) {
            String playerId = entry.getKey();
            GameSession session = entry.getValue();

            try {
                Integer currentScore = playerScores.get(playerId);
                if (currentScore != null) {
                    gameSessionService.updateSessionScore(session, currentScore);
                }
            } catch (Exception e) {
                LOGGER.warning("Error updating periodic session data for player " + playerId + ": " + e.getMessage());
            }
        }
    }

    // Method to get current online players count for statistics
    public int getOnlinePlayersCount() {
        return activeTanks.size();
    }

    private double[] findAvailableSpawnPoint() {
        for (double[] point : spawnPoints) {
            boolean occupied = false;
            for (Tank tank : activeTanks.values()) {
                if (Math.abs(tank.getX() - point[0]) < Tank.TANK_SIZE * 2
                        && Math.abs(tank.getY() - point[1]) < Tank.TANK_SIZE * 2) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied) {
                return point;
            }
        }
        return spawnPoints[RANDOM.nextInt(spawnPoints.length)];
    }

    private boolean isValidPosition(double x, double y, int size) {
        double halfSize = size / 2.0;
        return !(x - halfSize < 0 || x + halfSize > GAME_WIDTH
                || y - halfSize < 0 || y + halfSize > GAME_HEIGHT);
    }

    private boolean isOutOfBounds(double x, double y) {
        return x < 0 || x > GAME_WIDTH || y < 0 || y > GAME_HEIGHT;
    }

    private String generatePlayerColor() {
        String[] colors = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFA500", "#800080"};
        return colors[activeTanks.size() % colors.length];
    }

    private void broadcastGameState() {
        GameStateDTO.GameInfo gameInfo = new GameStateDTO.GameInfo(
                activeTanks.size(), gameStartTime, "ACTIVE"
        );

        GameStateDTO gameState = new GameStateDTO(
                new ArrayList<>(activeTanks.values()),
                new ArrayList<>(activeBullets),
                new HashMap<>(playerScores),
                new HashMap<>(playerStats),
                gameInfo
        );

        messagingTemplate.convertAndSend("/topic/gamestate", gameState);
    }

    public Map<String, Tank> getActiveTanks() {
        return new HashMap<>(activeTanks);
    }

    public List<Bullet> getActiveBullets() {
        return new ArrayList<>(activeBullets);
    }

    public Map<String, Integer> getPlayerScores() {
        return new HashMap<>(playerScores);
    }

    public Map<String, GameStateDTO.PlayerStats> getPlayerStats() {
        return new HashMap<>(playerStats);
    }
}
