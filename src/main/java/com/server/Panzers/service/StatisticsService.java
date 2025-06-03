package com.server.Panzers.service;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.Panzers.controller.LeaderboardController.GlobalStatsDTO;
import com.server.Panzers.model.GameStatistics;
import com.server.Panzers.model.User;
import com.server.Panzers.repository.GameStatisticsRepository;
import com.server.Panzers.repository.UserRepository;

@Service
public class StatisticsService {

    private final GameStatisticsRepository gameStatisticsRepository;
    private final UserRepository userRepository;
    private final GameService gameService;

    public StatisticsService(GameStatisticsRepository gameStatisticsRepository, UserRepository userRepository, @Lazy GameService gameService) { // Добавьте @Lazy здесь
        this.gameStatisticsRepository = gameStatisticsRepository;
        this.userRepository = userRepository;
        this.gameService = gameService;
    }

    public List<GameStatistics> getTopPlayersByScore() {
        return gameStatisticsRepository.findTopPlayersByScore();
    }

    public List<GameStatistics> getTopPlayersByWins() {
        return gameStatisticsRepository.findTopPlayersByWins();
    }

    public List<GameStatistics> getTopPlayersByKDRatio() {
        return gameStatisticsRepository.findTopPlayersByKDRatio();
    }

    public List<GameStatistics> getTopPlayersByAccuracy() {
        // Создаем кастомный запрос для сортировки по точности
        return gameStatisticsRepository.findAll().stream()
                .filter(gs -> gs.getShotsFired() > 0) // Только игроки, которые стреляли
                .sorted((a, b) -> Double.compare(b.getAccuracy(), a.getAccuracy()))
                .limit(50) // Топ 50
                .toList();
    }

    public GlobalStatsDTO getGlobalStats() {
        long totalPlayers = userRepository.count();
        long totalGames = gameStatisticsRepository.count();
        Double avgScore = gameStatisticsRepository.getAverageScore();
        long onlinePlayers = gameService.getOnlinePlayersCount();

        return new GlobalStatsDTO(
                totalPlayers,
                totalGames,
                avgScore != null ? avgScore : 0.0,
                onlinePlayers
        );
    }

    public GameStatistics getOrCreateUserStatistics(Long userId) {
        return gameStatisticsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        GameStatistics newStats = new GameStatistics(user);
                        return gameStatisticsRepository.save(newStats);
                    }
                    return null;
                });
    }

    public GameStatistics saveStatistics(GameStatistics statistics) {
        return gameStatisticsRepository.save(statistics);
    }

    public void updateRealTimeStats(Long userId, Integer currentScore, Integer kills, Integer deaths) {
        try {
            GameStatistics stats = getOrCreateUserStatistics(userId);
            if (stats != null) {
                // Update current session high score if needed
                if (currentScore != null && currentScore > stats.getHighestScore()) {
                    stats.setHighestScore(currentScore);
                    saveStatistics(stats);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating real-time stats: " + e.getMessage());
        }
    }
}
