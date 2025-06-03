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
        // Enhanced logic to determine win/loss/draw
        int kills = session.getKillsInSession();
        int deaths = session.getDeathsInSession();
        int finalScore = session.getFinalScore();
        double accuracy = session.getAccuracy();
        int durationMinutes = session.getDurationSeconds() / 60;

        // Advanced scoring system
        int gameScore = 0;

        // Score based on K/D ratio
        if (deaths == 0 && kills > 0) {
            gameScore += 50; // Perfect K/D
        } else if (kills > deaths * 2) {
            gameScore += 30; // Excellent K/D
        } else if (kills > deaths) {
            gameScore += 10; // Positive K/D
        } else if (kills < deaths) {
            gameScore -= 20; // Negative K/D
        }

        // Score based on accuracy
        if (accuracy >= 75.0) {
            gameScore += 20;
        } else if (accuracy >= 50.0) {
            gameScore += 10;
        } else if (accuracy < 25.0) {
            gameScore -= 10;
        }

        // Score based on final score
        if (finalScore >= 1000) {
            gameScore += 30;
        } else if (finalScore >= 500) {
            gameScore += 15;
        } else if (finalScore < 0) {
            gameScore -= 15;
        }

        // Score based on survival time
        if (durationMinutes >= 10) {
            gameScore += 10;
        } else if (durationMinutes >= 5) {
            gameScore += 5;
        }

        // Determine result based on total game score
        if (gameScore >= 40) {
            return GameSession.GameResult.WIN;
        } else if (gameScore <= -20) {
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
