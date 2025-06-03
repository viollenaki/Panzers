-- MySQL Database Schema for Panzers Game
-- This script creates the database structure for the tank game

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS panzers CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE panzers;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Game Statistics table
CREATE TABLE IF NOT EXISTS game_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_score BIGINT DEFAULT 0,
    total_games INT DEFAULT 0,
    wins INT DEFAULT 0,
    kills INT DEFAULT 0,
    deaths INT DEFAULT 0,
    shots_fired INT DEFAULT 0,
    shots_hit INT DEFAULT 0,
    accuracy DECIMAL(5,2) DEFAULT 0.00,
    total_time_played BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_total_score (total_score),
    INDEX idx_wins (wins),
    INDEX idx_kills (kills),
    INDEX idx_accuracy (accuracy)
);

-- Game Sessions table
CREATE TABLE IF NOT EXISTS game_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score INT DEFAULT 0,
    kills_in_session INT DEFAULT 0,
    deaths_in_session INT DEFAULT 0,
    shots_fired INT DEFAULT 0,
    shots_hit INT DEFAULT 0,
    accuracy DECIMAL(5,2) DEFAULT 0.00,
    session_duration BIGINT DEFAULT 0,
    game_result VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_game_result (game_result),
    INDEX idx_created_at (created_at),
    INDEX idx_score (score)
);

-- Sample data for testing (optional)
-- INSERT INTO users (username, email, password_hash) VALUES 
-- ('testuser', 'test@example.com', '$2a$10$...');

-- Show table structure
SHOW TABLES;
DESCRIBE users;
DESCRIBE game_statistics;
DESCRIBE game_sessions;

-- Show indexes
SHOW INDEX FROM users;
SHOW INDEX FROM game_statistics;
SHOW INDEX FROM game_sessions;
