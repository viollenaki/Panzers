package com.server.Panzers.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.Panzers.service.GameService;

@RestController
@RequestMapping("/api/game-stats")
public class GameStatsController {

    private final GameService gameService;

    public GameStatsController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/online-players")
    public ResponseEntity<Integer> getOnlinePlayersCount() {
        return ResponseEntity.ok(gameService.getOnlinePlayersCount());
    }

    @GetMapping("/top-players")
    public ResponseEntity<List<Map<String, Object>>> getTopPlayers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(gameService.getTopPlayers(limit));
    }

    @GetMapping("/current-winner")
    public ResponseEntity<String> getCurrentWinner() {
        String winner = gameService.determineGameWinner();
        return ResponseEntity.ok(winner != null ? winner : "No players in game");
    }

    @GetMapping("/active-tanks")
    public ResponseEntity<Map<String, Object>> getActiveTanks() {
        return ResponseEntity.ok(Map.of(
                "count", gameService.getActiveTanks().size(),
                "tanks", gameService.getActiveTanks()
        ));
    }

    @GetMapping("/player-scores")
    public ResponseEntity<Map<String, Integer>> getPlayerScores() {
        return ResponseEntity.ok(gameService.getPlayerScores());
    }
}
