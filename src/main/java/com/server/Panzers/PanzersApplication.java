package com.server.Panzers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PanzersApplication {

    public static void main(String[] args) {
        SpringApplication.run(PanzersApplication.class, args);
    }

}
