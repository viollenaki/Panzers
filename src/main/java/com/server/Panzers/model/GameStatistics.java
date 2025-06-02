package com.server.Panzers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_statistics")
public class GameStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_games")
    private Integer totalGames = 0;

    private Integer wins = 0;
    private Integer losses = 0;
    private Integer draws = 0;
    private Integer kills = 0;
    private Integer deaths = 0;

    @Column(name = "total_score")
    private Long totalScore = 0L;

    @Column(name = "highest_score")
    private Integer highestScore = 0;

    @Column(name = "total_playtime_seconds")
    private Long totalPlaytimeSeconds = 0L;

    @Column(name = "shots_fired")
    private Integer shotsFired = 0;

    @Column(name = "shots_hit")
    private Integer shotsHit = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public GameStatistics() {
    }

    public GameStatistics(User user) {
        this.user = user;
    }

    // Utility methods
    public void addGame(GameSession session) {
        totalGames++;
        switch (session.getGameResult()) {
            case WIN -> wins++;
            case LOSS -> losses++;
            case DRAW -> draws++;
            case DISCONNECT -> {} // No change for disconnect
        }
        kills += session.getKillsInSession();
        deaths += session.getDeathsInSession();
        totalScore += session.getFinalScore();
        if (session.getFinalScore() > highestScore) {
            highestScore = session.getFinalScore();
        }
        totalPlaytimeSeconds += session.getDurationSeconds();
    }

    public void addShot() {
        shotsFired++;
    }

    public void addHit() {
        shotsHit++;
    }

    public double getKDRatio() {
        return deaths > 0 ? (double) kills / deaths : kills;
    }

    public double getWinRate() {
        return totalGames > 0 ? (double) wins / totalGames * 100 : 0;
    }

    public double getAccuracy() {
        return shotsFired > 0 ? (double) shotsHit / shotsFired * 100 : 0;
    }

    public double getAverageScore() {
        return totalGames > 0 ? (double) totalScore / totalGames : 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(Integer totalGames) {
        this.totalGames = totalGames;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public Integer getDraws() {
        return draws;
    }

    public void setDraws(Integer draws) {
        this.draws = draws;
    }

    public Integer getKills() {
        return kills;
    }

    public void setKills(Integer kills) {
        this.kills = kills;
    }

    public Integer getDeaths() {
        return deaths;
    }

    public void setDeaths(Integer deaths) {
        this.deaths = deaths;
    }

    public Long getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Long totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(Integer highestScore) {
        this.highestScore = highestScore;
    }

    public Long getTotalPlaytimeSeconds() {
        return totalPlaytimeSeconds;
    }

    public void setTotalPlaytimeSeconds(Long totalPlaytimeSeconds) {
        this.totalPlaytimeSeconds = totalPlaytimeSeconds;
    }

    public Integer getShotsFired() {
        return shotsFired;
    }

    public void setShotsFired(Integer shotsFired) {
        this.shotsFired = shotsFired;
    }

    public Integer getShotsHit() {
        return shotsHit;
    }

    public void setShotsHit(Integer shotsHit) {
        this.shotsHit = shotsHit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Convenience method for JSON serialization
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }
}
