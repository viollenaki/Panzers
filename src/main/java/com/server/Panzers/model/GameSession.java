package com.server.Panzers.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_start")
    private LocalDateTime sessionStart;

    @Column(name = "session_end")
    private LocalDateTime sessionEnd;

    @Column(name = "final_score")
    private Integer finalScore = 0;

    @Column(name = "kills_in_session")
    private Integer killsInSession = 0;

    @Column(name = "deaths_in_session")
    private Integer deathsInSession = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_result")
    private GameResult gameResult = GameResult.DISCONNECT;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @PrePersist
    protected void onCreate() {
        sessionStart = LocalDateTime.now();
    }

    // Constructors
    public GameSession() {
    }

    public GameSession(User user) {
        this.user = user;
        this.sessionStart = LocalDateTime.now();
    }

    // Utility methods
    public void endSession(GameResult result) {
        this.sessionEnd = LocalDateTime.now();
        this.gameResult = result;
        this.durationSeconds = (int) ChronoUnit.SECONDS.between(sessionStart, sessionEnd);
    }

    public void addKill() {
        killsInSession++;
        finalScore += 100; // 100 points per kill
    }

    public void addDeath() {
        deathsInSession++;
        finalScore -= 50; // Lose 50 points per death
    }

    public void addScore(int points) {
        finalScore += points;
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

    public LocalDateTime getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(LocalDateTime sessionStart) {
        this.sessionStart = sessionStart;
    }

    public LocalDateTime getSessionEnd() {
        return sessionEnd;
    }

    public void setSessionEnd(LocalDateTime sessionEnd) {
        this.sessionEnd = sessionEnd;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }

    public Integer getKillsInSession() {
        return killsInSession;
    }

    public void setKillsInSession(Integer killsInSession) {
        this.killsInSession = killsInSession;
    }

    public Integer getDeathsInSession() {
        return deathsInSession;
    }

    public void setDeathsInSession(Integer deathsInSession) {
        this.deathsInSession = deathsInSession;
    }

    public GameResult getGameResult() {
        return gameResult;
    }

    public void setGameResult(GameResult gameResult) {
        this.gameResult = gameResult;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public enum GameResult {
        WIN, LOSS, DRAW, DISCONNECT
    }
}
