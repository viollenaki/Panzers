package com.server.Panzers.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.concurrent.atomic.AtomicLong;

public class Tank {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final String id;
    private String playerId;
    private double x;
    private double y;
    private Direction direction;
    private int health;
    private int ammunition;
    private double speed;
    private String color;
    private boolean isMoving;
    private long lastShotTime;
    private boolean isAlive;
    private double angle; // rotation angle in radians

    // Constants
    public static final int MAX_HEALTH = 100;
    public static final int MAX_AMMUNITION = 30;
    public static final double DEFAULT_SPEED = 2.0;
    public static final int TANK_SIZE = 30;
    public static final long FIRE_RATE_MS = 500; // 2 shots per second
    public static final int DAMAGE_PER_HIT = 25;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public Tank() {
        this.id = "tank_" + ID_GENERATOR.incrementAndGet();
        this.health = MAX_HEALTH;
        this.ammunition = MAX_AMMUNITION;
        this.speed = DEFAULT_SPEED;
        this.direction = Direction.UP;
        this.isMoving = false;
        this.lastShotTime = 0;
        this.isAlive = true;
        this.angle = 0; // facing right initially
    }

    public Tank(String playerId, double x, double y, String color) {
        this();
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    // Movement methods
    public void move(Direction newDirection) {
        this.direction = newDirection;
        this.isMoving = true;
        
        switch (direction) {
            case UP:
                y -= speed;
                break;
            case DOWN:
                y += speed;
                break;
            case LEFT:
                x -= speed;
                break;
            case RIGHT:
                x += speed;
                break;
        }
    }

    public void stop() {
        this.isMoving = false;
    }

    public void stopMoving() {
        this.isMoving = false;
    }

    // Combat methods
    public Bullet shoot() {
        if (ammunition <= 0 || !canShoot()) {
            return null;
        }

        ammunition--;
        lastShotTime = System.currentTimeMillis();

        double bulletX = x;
        double bulletY = y;

        // Position bullet at tank's edge based on direction
        switch (direction) {
            case UP:
                bulletY -= TANK_SIZE / 2;
                break;
            case DOWN:
                bulletY += TANK_SIZE / 2;
                break;
            case LEFT:
                bulletX -= TANK_SIZE / 2;
                break;
            case RIGHT:
                bulletX += TANK_SIZE / 2;
                break;
        }

        return new Bullet(bulletX, bulletY, direction, playerId);
    }

    @JsonIgnore
    public boolean canShoot() {
        return ammunition > 0 && 
               (System.currentTimeMillis() - lastShotTime) >= FIRE_RATE_MS;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            isAlive = false;
        }
    }

    public void reload() {
        this.ammunition = MAX_AMMUNITION;
    }

    public void respawn(double spawnX, double spawnY) {
        this.x = spawnX;
        this.y = spawnY;
        this.health = MAX_HEALTH;
        this.ammunition = MAX_AMMUNITION;
        this.isAlive = true;
        this.direction = Direction.UP;
        this.isMoving = false;
    }

    // Collision detection
    public boolean intersects(Tank other) {
        return Math.abs(this.x - other.x) < TANK_SIZE
                && Math.abs(this.y - other.y) < TANK_SIZE;
    }

    public boolean intersects(double otherX, double otherY, int otherSize) {
        return Math.abs(this.x - otherX) < (TANK_SIZE + otherSize) / 2.0
                && Math.abs(this.y - otherY) < (TANK_SIZE + otherSize) / 2.0;
    }

    // Utility methods
    public boolean needsAmmoReload() {
        return ammunition == 0;
    }

    public double getHealthPercentage() {
        return (double) health / MAX_HEALTH * 100;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
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

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public long getLastShotTime() {
        return lastShotTime;
    }

    public void setLastShotTime(long lastShotTime) {
        this.lastShotTime = lastShotTime;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    // Update direction based on angle
    public void updateDirectionFromAngle() {
        double normalizedAngle = angle;
        while (normalizedAngle < 0) normalizedAngle += Math.PI * 2;
        while (normalizedAngle >= Math.PI * 2) normalizedAngle -= Math.PI * 2;
        
        if (normalizedAngle >= 7 * Math.PI / 4 || normalizedAngle < Math.PI / 4) {
            this.direction = Direction.RIGHT;
        } else if (normalizedAngle >= Math.PI / 4 && normalizedAngle < 3 * Math.PI / 4) {
            this.direction = Direction.DOWN;
        } else if (normalizedAngle >= 3 * Math.PI / 4 && normalizedAngle < 5 * Math.PI / 4) {
            this.direction = Direction.LEFT;
        } else {
            this.direction = Direction.UP;
        }
    }
}
