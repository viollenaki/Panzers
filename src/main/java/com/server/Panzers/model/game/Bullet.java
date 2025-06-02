package com.server.Panzers.model.game;

import java.util.concurrent.atomic.AtomicLong;

public class Bullet {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final String id;
    private double x;
    private double y;
    private Tank.Direction direction;
    private double speed;
    private String ownerId;
    private int damage;
    private long createdTime;
    private boolean isActive;

    // Constants
    public static final double DEFAULT_SPEED = 5.0;
    public static final int BULLET_SIZE = 5;
    public static final int DEFAULT_DAMAGE = 25;
    public static final long MAX_LIFETIME_MS = 5000; // 5 seconds
    public static final double MAX_TRAVEL_DISTANCE = 800; // pixels

    private double initialX;
    private double initialY;

    public Bullet() {
        this.id = "bullet_" + ID_GENERATOR.incrementAndGet();
        this.speed = DEFAULT_SPEED;
        this.damage = DEFAULT_DAMAGE;
        this.createdTime = System.currentTimeMillis();
        this.isActive = true;
    }

    public Bullet(double x, double y, Tank.Direction direction, String ownerId) {
        this();
        this.x = x;
        this.y = y;
        this.initialX = x;
        this.initialY = y;
        this.direction = direction;
        this.ownerId = ownerId;
    }

    public void update() {
        if (!isActive) {
            return;
        }

        // Move bullet in its direction
        switch (direction) {
            case UP ->
                y -= speed;
            case DOWN ->
                y += speed;
            case LEFT ->
                x -= speed;
            case RIGHT ->
                x += speed;
        }

        // Check if bullet should be destroyed
        if (shouldDestroy()) {
            isActive = false;
        }
    }

    private boolean shouldDestroy() {
        // Destroy if exceeded lifetime
        if (System.currentTimeMillis() - createdTime > MAX_LIFETIME_MS) {
            return true;
        }

        // Destroy if traveled too far
        double travelDistance = Math.sqrt(
                Math.pow(x - initialX, 2) + Math.pow(y - initialY, 2)
        );

        return travelDistance > MAX_TRAVEL_DISTANCE;
    }

    // Collision detection
    public boolean intersects(Tank tank) {
        if (tank.getPlayerId().equals(ownerId)) {
            return false; // Can't hit own tank
        }

        return Math.abs(this.x - tank.getX()) < (BULLET_SIZE + Tank.TANK_SIZE) / 2.0
                && Math.abs(this.y - tank.getY()) < (BULLET_SIZE + Tank.TANK_SIZE) / 2.0;
    }

    public boolean intersects(double wallX, double wallY, int wallSize) {
        return Math.abs(this.x - wallX) < (BULLET_SIZE + wallSize) / 2.0
                && Math.abs(this.y - wallY) < (BULLET_SIZE + wallSize) / 2.0;
    }

    public void destroy() {
        this.isActive = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
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

    public Tank.Direction getDirection() {
        return direction;
    }

    public void setDirection(Tank.Direction direction) {
        this.direction = direction;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
