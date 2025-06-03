package com.server.Panzers.dto;

public class AchievementDTO {
    
    private String type = "ACHIEVEMENT_UNLOCKED";
    private String playerId;
    private String achievementName;
    private String description;
    private int bonusPoints;
    private long timestamp;
    
    public AchievementDTO() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public AchievementDTO(String playerId, String achievementName, String description, int bonusPoints) {
        this();
        this.playerId = playerId;
        this.achievementName = achievementName;
        this.description = description;
        this.bonusPoints = bonusPoints;
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
    
    public String getAchievementName() {
        return achievementName;
    }
    
    public void setAchievementName(String achievementName) {
        this.achievementName = achievementName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getBonusPoints() {
        return bonusPoints;
    }
    
    public void setBonusPoints(int bonusPoints) {
        this.bonusPoints = bonusPoints;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
