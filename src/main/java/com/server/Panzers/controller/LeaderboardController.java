package com.server.Panzers.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.Panzers.model.GameStatistics;
import com.server.Panzers.service.StatisticsService;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    private final StatisticsService statisticsService;

    public LeaderboardController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/leaderboard/{type}")
    public ResponseEntity<List<GameStatistics>> getLeaderboard(@PathVariable String type) {
        try {
            List<GameStatistics> leaderboard;

            switch (type.toLowerCase()) {
                case "score":
                    leaderboard = statisticsService.getTopPlayersByScore();
                    break;
                case "wins":
                    leaderboard = statisticsService.getTopPlayersByWins();
                    break;
                case "kd":
                    leaderboard = statisticsService.getTopPlayersByKDRatio();
                    break;
                case "accuracy":
                    leaderboard = statisticsService.getTopPlayersByAccuracy();
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats/global")
    public ResponseEntity<GlobalStatsDTO> getGlobalStats() {
        try {
            GlobalStatsDTO stats = statisticsService.getGlobalStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // DTO for global statistics
    public static class GlobalStatsDTO {

        private long totalPlayers;
        private long totalGames;
        private double averageScore;
        private long onlinePlayers;

        public GlobalStatsDTO() {
        }

        public GlobalStatsDTO(long totalPlayers, long totalGames, double averageScore, long onlinePlayers) {
            this.totalPlayers = totalPlayers;
            this.totalGames = totalGames;
            this.averageScore = averageScore;
            this.onlinePlayers = onlinePlayers;
        }

        // Getters and Setters
        public long getTotalPlayers() {
            return totalPlayers;
        }

        public void setTotalPlayers(long totalPlayers) {
            this.totalPlayers = totalPlayers;
        }

        public long getTotalGames() {
            return totalGames;
        }

        public void setTotalGames(long totalGames) {
            this.totalGames = totalGames;
        }

        public double getAverageScore() {
            return averageScore;
        }

        public void setAverageScore(double averageScore) {
            this.averageScore = averageScore;
        }

        public long getOnlinePlayers() {
            return onlinePlayers;
        }

        public void setOnlinePlayers(long onlinePlayers) {
            this.onlinePlayers = onlinePlayers;
        }
    }
}
