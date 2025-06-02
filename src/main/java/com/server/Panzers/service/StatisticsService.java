package com.server.Panzers.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.server.Panzers.controller.LeaderboardController.GlobalStatsDTO;
import com.server.Panzers.model.GameStatistics;
import com.server.Panzers.repository.GameStatisticsRepository;
import com.server.Panzers.repository.UserRepository;

@Service
public class StatisticsService {

    private final GameStatisticsRepository gameStatisticsRepository;
    private final UserRepository userRepository;

    public StatisticsService(GameStatisticsRepository gameStatisticsRepository, UserRepository userRepository) {
        this.gameStatisticsRepository = gameStatisticsRepository;
        this.userRepository = userRepository;
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
        long activeUsers = gameStatisticsRepository.countActivePlayersWithMinGames(1);
        Double avgScore = gameStatisticsRepository.getAverageScore();

        // Для простоты считаем online игроков как активных пользователей
        // В реальном приложении это было бы основано на сессиях WebSocket
        long onlinePlayers = Math.min(activeUsers / 10, activeUsers); // Примерно 10% от активных

        return new GlobalStatsDTO(
                totalPlayers,
                activeUsers, // используем как totalGames для простоты
                avgScore != null ? avgScore : 0.0,
                onlinePlayers
        );
    }

    public GameStatistics getOrCreateUserStatistics(Long userId) {
        return gameStatisticsRepository.findByUserId(userId)
                .orElse(null);
    }

    public GameStatistics saveStatistics(GameStatistics statistics) {
        return gameStatisticsRepository.save(statistics);
    }
}
