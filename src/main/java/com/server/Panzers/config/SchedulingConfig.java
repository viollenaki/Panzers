package com.server.Panzers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.server.Panzers.service.GameSessionService;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private final GameSessionService gameSessionService;

    public SchedulingConfig(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldSessions() {
        gameSessionService.cleanupOldSessions();
    }
}
