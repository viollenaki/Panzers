package com.server.Panzers.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.server.Panzers.service.GameService;

@Controller
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/game")
    public String game(Model model) {
        model.addAttribute("activePlayers", gameService.getActiveTanks().size());
        return "game";
    }

    @GetMapping("/leaderboard")
    public String leaderboard() {
        return "leaderboard";
    }

    @GetMapping("/game/join")
    public String joinGame() {
        return "game"; // логика для присоединения к игре
    }
}
