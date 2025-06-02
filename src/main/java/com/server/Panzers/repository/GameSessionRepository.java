package com.server.Panzers.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.server.Panzers.model.GameSession;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    List<GameSession> findByUserIdOrderBySessionStartDesc(Long userId);

    List<GameSession> findBySessionStartBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.sessionStart >= :date")
    long countGamesFromDate(@Param("date") LocalDateTime date);

    @Query("SELECT AVG(gs.durationSeconds) FROM GameSession gs WHERE gs.sessionEnd IS NOT NULL")
    Double getAverageGameDuration();

    @Query("SELECT gs FROM GameSession gs WHERE gs.user.id = :userId ORDER BY gs.finalScore DESC")
    List<GameSession> findUserBestScores(@Param("userId") Long userId);

    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.gameResult = :result AND gs.sessionStart >= :date")
    long countGamesByResultFromDate(@Param("result") GameSession.GameResult result, @Param("date") LocalDateTime date);

    @Query("SELECT gs FROM GameSession gs WHERE gs.sessionEnd IS NULL")
    List<GameSession> findActiveSessions();
}
