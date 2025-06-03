package com.server.Panzers.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.Panzers.model.GameSession;
import com.server.Panzers.model.GameStatistics;
import com.server.Panzers.model.User;
import com.server.Panzers.repository.GameSessionRepository;

@Service
@Transactional
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final StatisticsService statisticsService;

    public GameSessionService(GameSessionRepository gameSessionRepository, StatisticsService statisticsService) {
        this.gameSessionRepository = gameSessionRepository;
        this.statisticsService = statisticsService;
    }

    public GameSession createSession(User user) {
        GameSession session = new GameSession(user);
        return gameSessionRepository.save(session);
    }

    public void recordKill(GameSession session, int scoreGain) {
        session.addKill();
        session.addScore(scoreGain);
        gameSessionRepository.save(session);
    }

    public void recordDeath(GameSession session) {
        session.addDeath();
        gameSessionRepository.save(session);
    }

    public void recordShot(GameSession session) {
        session.addShot();
        gameSessionRepository.save(session);
    }

    public void recordHit(GameSession session) {
        session.addHit();
        gameSessionRepository.save(session);
    }

    public void updateSessionScore(GameSession session, int newScore) {
        session.setFinalScore(newScore);
        gameSessionRepository.save(session);
    }

    public GameSession endSession(GameSession session, int finalScore) {
        session.setFinalScore(finalScore);

        // Determine game result based on performance
        GameSession.GameResult result = determineGameResult(session);
        session.endSession(result);

        // Save the completed session
        GameSession savedSession = gameSessionRepository.save(session);

        // Update user statistics
        updateUserStatistics(session);

        return savedSession;
    }

    private GameSession.GameResult determineGameResult(GameSession session) {
        // Simple logic to determine win/loss/draw
        int kills = session.getKillsInSession();
        int deaths = session.getDeathsInSession();

        if (kills > deaths + 2) {
            return GameSession.GameResult.WIN;
        } else if (deaths > kills + 2) {
            return GameSession.GameResult.LOSS;
        } else {
            return GameSession.GameResult.DRAW;
        }
    }

    private void updateUserStatistics(GameSession session) {
        try {
            User user = session.getUser();
            GameStatistics stats = statisticsService.getOrCreateUserStatistics(user.getId());

            if (stats == null) {
                // Create new statistics for user
                stats = new GameStatistics(user);
            }

            // Update statistics with session data
            stats.addGame(session);
            stats.setShotsFired(stats.getShotsFired() + session.getShotsFired());
            stats.setShotsHit(stats.getShotsHit() + session.getShotsHit());

            // Save updated statistics
            statisticsService.saveStatistics(stats);

        } catch (Exception e) {
            System.err.println("Error updating user statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void cleanupOldSessions() {
        // Clean up sessions older than 24 hours that are still active
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        try {
            var activeSessions = gameSessionRepository.findActiveSessions();
            for (GameSession session : activeSessions) {
                if (session.getSessionStart().isBefore(cutoff)) {
                    session.endSession(GameSession.GameResult.DISCONNECT);
                    gameSessionRepository.save(session);
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up old sessions: " + e.getMessage());
        }
    }
}
