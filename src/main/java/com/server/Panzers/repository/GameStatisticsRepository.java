package com.server.Panzers.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.server.Panzers.model.GameStatistics;

@Repository
public interface GameStatisticsRepository extends JpaRepository<GameStatistics, Long> {

    Optional<GameStatistics> findByUserId(Long userId);

    @Query("SELECT gs FROM GameStatistics gs ORDER BY gs.totalScore DESC")
    List<GameStatistics> findTopPlayersByScore();

    @Query("SELECT gs FROM GameStatistics gs ORDER BY gs.wins DESC")
    List<GameStatistics> findTopPlayersByWins();

    @Query("SELECT gs FROM GameStatistics gs WHERE gs.totalGames > 0 ORDER BY (CAST(gs.wins AS DOUBLE) / gs.totalGames) DESC")
    List<GameStatistics> findTopPlayersByWinRate();

    @Query("SELECT gs FROM GameStatistics gs WHERE gs.deaths > 0 ORDER BY (CAST(gs.kills AS DOUBLE) / gs.deaths) DESC")
    List<GameStatistics> findTopPlayersByKDRatio();

    @Query("SELECT AVG(gs.totalScore) FROM GameStatistics gs WHERE gs.totalGames > 0")
    Double getAverageScore();

    @Query("SELECT COUNT(gs) FROM GameStatistics gs WHERE gs.totalGames >= :minGames")
    long countActivePlayersWithMinGames(@Param("minGames") int minGames);
}
