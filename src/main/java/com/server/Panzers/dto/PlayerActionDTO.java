package com.server.Panzers.dto;

import com.server.Panzers.model.game.Tank;

public class PlayerActionDTO {

    private String type;
    private String playerId;
    private long timestamp;
    private ActionData data;

    public enum ActionType {
        PLAYER_JOIN, PLAYER_MOVE, PLAYER_SHOOT, PLAYER_STOP, PLAYER_LEAVE
    }

    public static class ActionData {

        private double x;
        private double y;
        private Tank.Direction direction;
        private boolean isMoving;
        private String playerName;
        private String color;
        private double angle; // Add angle support for smooth rotation

        // Constructors
        public ActionData() {
        }

        public ActionData(double x, double y, Tank.Direction direction, boolean isMoving) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.isMoving = isMoving;
        }

        // Getters and Setters
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

        public void setDirection(String directionStr) {
            if (directionStr != null) {
                try {
                    this.direction = Tank.Direction.valueOf(directionStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    this.direction = null;
                }
            }
        }

        public boolean isMoving() {
            return isMoving;
        }

        public void setMoving(boolean moving) {
            isMoving = moving;
        }

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public double getAngle() {
            return angle;
        }

        public void setAngle(double angle) {
            this.angle = angle;
        }
    }

    // Constructors
    public PlayerActionDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public PlayerActionDTO(String type, String playerId, ActionData data) {
        this();
        this.type = type;
        this.playerId = playerId;
        this.data = data;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ActionData getData() {
        return data;
    }

    public void setData(ActionData data) {
        this.data = data;
    }
}
