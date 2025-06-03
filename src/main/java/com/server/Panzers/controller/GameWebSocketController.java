package com.server.Panzers.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.server.Panzers.dto.PlayerActionDTO;
import com.server.Panzers.service.GameService;

@Controller
public class GameWebSocketController {

    private final GameService gameService;

    public GameWebSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/game/action")
    public void handlePlayerAction(@Payload PlayerActionDTO action, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // Use the playerId from the action if available, otherwise use session ID
            if (action.getPlayerId() == null || action.getPlayerId().isEmpty()) {
                String sessionId = headerAccessor.getSessionId();
                action.setPlayerId(sessionId);
            }

            // Handle the action
            gameService.handlePlayerAction(action);
        } catch (Exception e) {
            System.err.println("Error handling player action: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/game/join")
    public void handlePlayerJoin(@Payload PlayerActionDTO action, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            if (action.getPlayerId() == null || action.getPlayerId().isEmpty()) {
                action.setPlayerId(sessionId);
            }
            action.setType("PLAYER_JOIN");

            gameService.handlePlayerAction(action);
        } catch (Exception e) {
            System.err.println("Error handling player join: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/game/leave")
    public void handlePlayerLeave(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();

            PlayerActionDTO action = new PlayerActionDTO();
            action.setPlayerId(sessionId);
            action.setType("PLAYER_LEAVE");

            gameService.handlePlayerAction(action);
        } catch (Exception e) {
            System.err.println("Error handling player leave: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
