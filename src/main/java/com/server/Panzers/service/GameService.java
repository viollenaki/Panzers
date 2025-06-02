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
import com.server.Panzers.model.game.Bullet;
import com.server.Panzers.model.game.Tank;
import com.server.Panzers.model.game.Tank.Direction;

@Service
public class GameService {

    private static final Logger LOGGER = Logger.getLogger(GameService.class.getName());
    private static final Random RANDOM = new Random();

    private final SimpMessagingTemplate messagingTemplate;

    public GameService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Game state storage
    private final Map<String, Tank> activeTanks = new ConcurrentHashMap<>();
    private final List<Bullet> activeBullets = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();
    private final Map<String, GameStateDTO.PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Map<String, String> playerNames = new ConcurrentHashMap<>();

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
        playerNames.put(playerId, data.getPlayerName() != null ? data.getPlayerName() : "Player" + playerId.substring(0, 6));
        playerScores.put(playerId, 0);
        playerStats.put(playerId, new GameStateDTO.PlayerStats(
                playerNames.get(playerId), 0, 0, Tank.MAX_HEALTH, Tank.MAX_AMMUNITION, true
        ));

        broadcastGameState();
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

        // Server-side validation
        if (isValidPosition(newX, newY, Tank.TANK_SIZE)) {
            // Check for collisions with other tanks
            boolean collisionDetected = false;
            for (Tank otherTank : activeTanks.values()) {
                if (!otherTank.getId().equals(tank.getId())
                        && otherTank.isAlive()
                        && tank.intersects(newX, newY, Tank.TANK_SIZE)) {
                    collisionDetected = true;
                    break;
                }
            }

            if (!collisionDetected) {
                tank.setX(newX);
                tank.setY(newY);
                tank.setDirection(Tank.Direction.valueOf(direction.name()));
                tank.setMoving(isMoving);
            }
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
        }
    }

    private void handlePlayerStop(String playerId) {
        Tank tank = activeTanks.get(playerId);
        if (tank != null) {
            tank.stop();
            // Update player stats if needed
            GameStateDTO.PlayerStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.setMoving(false);
            }
        }
    }

    private void handlePlayerReload(String playerId) {
        Tank tank = activeTanks.get(playerId);
        if (tank == null || !tank.isAlive()) {
            return;
        }

        // Check if tank can reload
        if (tank.getAmmunition() < Tank.MAX_AMMUNITION) {
            tank.reload();

            // Update player stats
            GameStateDTO.PlayerStats stats = playerStats.get(playerId);
            if (stats != null) {
                stats.setAmmunition(tank.getAmmunition());
            }

            // Broadcast updated state
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

            // Remove bullets that are out of bounds or inactive
            if (!bullet.isActive() || isOutOfBounds(bullet.getX(), bullet.getY())) {
                bulletIterator.remove();
            }
        }
    }

    private void checkCollisions() {
        Iterator<Bullet> bulletIterator = activeBullets.iterator();

        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();

            // Check bullet vs tank collisions
            for (Tank tank : activeTanks.values()) {
                if (bullet.intersects(tank) && tank.isAlive()) {
                    // Apply damage
                    tank.takeDamage(bullet.getDamage());

                    // Update player stats
                    GameStateDTO.PlayerStats targetStats = playerStats.get(tank.getPlayerId());
                    if (targetStats != null) {
                        targetStats.setHealth(tank.getHealth());

                        if (!tank.isAlive()) {
                            targetStats.setAlive(false);
                            targetStats.setDeaths(targetStats.getDeaths() + 1);

                            // Award kill to shooter
                            GameStateDTO.PlayerStats shooterStats = playerStats.get(bullet.getOwnerId());
                            if (shooterStats != null) {
                                shooterStats.setKills(shooterStats.getKills() + 1);
                                playerScores.put(bullet.getOwnerId(),
                                        playerScores.getOrDefault(bullet.getOwnerId(), 0) + 100);
                            }

                            // Respawn tank after delay
                            respawnTank(tank);
                        }
                    }

                    bullet.destroy();
                    bulletIterator.remove();
                    break;
                }
            }

            // Check bullet vs wall collisions (basic boundary check)
            if (bullet.getX() <= 0 || bullet.getX() >= GAME_WIDTH
                    || bullet.getY() <= 0 || bullet.getY() >= GAME_HEIGHT) {
                bullet.destroy();
                bulletIterator.remove();
            }
        }
    }

    private void respawnTank(Tank tank) {
        // Respawn after 3 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                double[] spawnPoint = findAvailableSpawnPoint();
                tank.respawn(spawnPoint[0], spawnPoint[1]);

                GameStateDTO.PlayerStats stats = playerStats.get(tank.getPlayerId());
                if (stats != null) {
                    stats.setAlive(true);
                    stats.setHealth(Tank.MAX_HEALTH);
                    stats.setAmmunition(Tank.MAX_AMMUNITION);
                }
            }
        }, 3000);
    }

    private void handlePlayerLeave(String playerId) {
        // Remove the player's tank from active tanks
        activeTanks.remove(playerId);

        // Remove player stats and scores
        playerStats.remove(playerId);
        playerScores.remove(playerId);
        playerNames.remove(playerId);

        // Broadcast updated game state to all players
        broadcastGameState();
    }

    private double[] findAvailableSpawnPoint() {
        // Find spawn point that's not occupied by other tanks
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
        // If all spawn points occupied, return random point
        return spawnPoints[RANDOM.nextInt(spawnPoints.length)];
    }

    private boolean isValidPosition(double x, double y, int size) {
        // Check game boundaries
        double halfSize = size / 2.0;
        return !(x - halfSize < 0 || x + halfSize > GAME_WIDTH
                || y - halfSize < 0 || y + halfSize > GAME_HEIGHT);

        // Add wall collision detection here if you have walls
        // Check against static obstacles, destructible walls, etc.
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

    // Getters for controllers
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
