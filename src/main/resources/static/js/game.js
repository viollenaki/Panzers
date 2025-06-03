// Tank Game Client-Side Engine
class TankGame {
    constructor() {
        this.canvas = document.getElementById('gameCanvas');
        this.ctx = this.canvas.getContext('2d');
        this.stompClient = null;
        this.sessionId = null;
        this.playerTank = null;
        this.gameState = null;
        this.keyStates = {};
        this.lastMoveTime = 0;
        this.lastShootTime = 0;
        this.gameStartTime = Date.now();
        this.movementInterval = null;
        this.isPaused = false;
        
        // Game constants
        this.CANVAS_WIDTH = 800;
        this.CANVAS_HEIGHT = 600;
        this.TANK_SIZE = 30;
        this.BULLET_SIZE = 5;
        this.MOVE_THRESHOLD = 16; // ~60 FPS movement updates
        this.SHOOT_COOLDOWN = 500; // 500ms between shots
        
        // Tank physics constants
        this.MAX_SPEED = 3.0;
        this.ACCELERATION = 0.15;
        this.FRICTION = 0.9;
        this.ROTATION_SPEED = 0.08; // radians per frame
        this.MIN_SPEED_THRESHOLD = 0.1;
        
        // Local tank state for smooth movement
        this.localTank = {
            x: 0,
            y: 0,
            angle: 0, // rotation in radians
            velocityX: 0,
            velocityY: 0,
            speed: 0,
            targetSpeed: 0,
            isMoving: false
        };
        
        this.initializeGame();
    }

    initializeGame() {
        console.log('Starting Tank Game initialization...');
        
        // Setup canvas
        this.setupCanvas();
        
        // Setup keyboard handlers
        this.setupKeyboardHandlers();
        
        // Connect to WebSocket
        this.connectWebSocket();
        
        // Start game loop
        this.startGameLoop();
        
        console.log('Tank Game initialized successfully');
    }

    connectWebSocket() {
        console.log('Connecting to WebSocket...');
        
        // Используем StompJs (новая версия библиотеки)
        const socket = new SockJS('/ws');
        this.stompClient = new StompJs.Client({
            webSocketFactory: () => socket,
            connectHeaders: {},
            debug: (str) => {
                console.log('STOMP: ' + str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        // Обработчики подключения
        this.stompClient.onConnect = (frame) => {
            console.log('Connected to WebSocket: ' + frame);
            this.onWebSocketConnected();
        };

        this.stompClient.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
            this.updateConnectionStatus('error', 'Connection Error');
        };

        this.stompClient.onWebSocketClose = (event) => {
            console.log('WebSocket connection closed');
            this.updateConnectionStatus('error', 'Disconnected');
        };

        this.stompClient.onWebSocketError = (event) => {
            console.error('WebSocket error:', event);
            this.updateConnectionStatus('error', 'Connection Error');
        };

        // Обновляем статус подключения
        this.updateConnectionStatus('connecting', 'Connecting...');
        
        // Активируем клиент
        this.stompClient.activate();
    }    onWebSocketConnected() {
        console.log('WebSocket connected successfully');
        this.updateConnectionStatus('connected', 'Connected');
        
        // Получаем session ID (для упрощения используем временную метку)
        if (!this.sessionId) {
            this.sessionId = 'player_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        }
        
        // Подписываемся на игровые события
        this.stompClient.subscribe('/topic/gamestate', (message) => {
            this.handleGameStateUpdate(JSON.parse(message.body));
        });
        
        // Подписываемся на уведомления о достижениях
        this.stompClient.subscribe('/topic/achievements', (message) => {
            this.handleAchievementNotification(JSON.parse(message.body));
        });
        
        // Подписываемся на персональные уведомления о достижениях
        this.stompClient.subscribe('/user/queue/achievements', (message) => {
            this.handlePersonalAchievement(JSON.parse(message.body));
        });
        
        // Присоединяемся к игре
        this.joinGame();
    }

    joinGame() {
        if (!this.stompClient || !this.stompClient.connected) {
            console.error('Cannot join game: WebSocket not connected');
            return;
        }
        
        console.log('Joining game...');
        this.sendPlayerAction('PLAYER_JOIN', {
            timestamp: Date.now()
        });
    }

    sendPlayerAction(actionType, data = {}) {
        if (!this.stompClient || !this.stompClient.connected) {
            console.warn('Cannot send action: WebSocket not connected');
            return;
        }

        const action = {
            type: actionType,
            playerId: this.sessionId, // Сервер использует playerId
            timestamp: Date.now(),
            data: data
        };

        try {
            this.stompClient.publish({
                destination: '/app/game/action',
                body: JSON.stringify(action)
            });
        } catch (error) {
            console.error('Error sending player action:', error);
        }
    }

    updateConnectionStatus(status, text) {
        const indicator = document.getElementById('statusIndicator');
        const statusText = document.getElementById('statusText');
        
        if (indicator) {
            indicator.className = 'status-indicator ' + status;
        }
        if (statusText) {
            statusText.textContent = text;
        }
    }

    setupCanvas() {
        if (!this.canvas) {
            console.error('Game canvas not found!');
            return;
        }
        
        this.canvas.width = this.CANVAS_WIDTH;
        this.canvas.height = this.CANVAS_HEIGHT;
        
        // Начальная отрисовка
        this.renderGame();
    }

    setupKeyboardHandlers() {
        // Track key states for smooth movement
        const validKeys = [
            'KeyW', 'KeyA', 'KeyS', 'KeyD', // Movement and rotation
            'Space', 'Enter', // Shooting
            'Escape', 'KeyP', // Pause/Menu
            'KeyR' // Reload
        ];
        
        document.addEventListener('keydown', (event) => {
            if (validKeys.includes(event.code)) {
                event.preventDefault();
                this.handleKeyDown(event.code);
            }
        });
        
        document.addEventListener('keyup', (event) => {
            if (validKeys.includes(event.code)) {
                event.preventDefault();
                this.handleKeyUp(event.code);
            }
        });
        
        // Handle window focus/blur for movement
        window.addEventListener('blur', () => {
            this.stopAllMovement();
        });

        window.addEventListener('focus', () => {
            // Clear all key states when window regains focus
            this.keyStates = {};
        });
    }

    handleKeyDown(keyCode) {
        if (this.keyStates[keyCode]) return; // Already pressed
        
        this.keyStates[keyCode] = true;
        
        // Handle immediate actions (non-movement)
        switch (keyCode) {
            case 'Space':
            case 'Enter':
                this.shoot();
                break;
            case 'Escape':
            case 'KeyP':
                this.togglePause();
                break;
            case 'KeyR':
                this.requestReload();
                break;
        }

        // Start continuous movement if movement key pressed
        if (this.isMovementKey(keyCode)) {
            this.startMovement();
        }
    }

    handleKeyUp(keyCode) {
        this.keyStates[keyCode] = false;
        
        // Stop movement if no movement keys are pressed
        if (this.isMovementKey(keyCode) && !this.hasActiveMovementKeys()) {
            this.stopMovement();
        }
    }

    isMovementKey(keyCode) {
        return ['KeyW', 'KeyA', 'KeyS', 'KeyD'].includes(keyCode);
    }

    hasActiveMovementKeys() {
        const movementKeys = ['KeyW', 'KeyA', 'KeyS', 'KeyD'];
        return movementKeys.some(key => this.keyStates[key]);
    }

    startMovement() {
        if (this.movementInterval) return; // Already moving
        
        this.movementInterval = setInterval(() => {
            this.updateTankPhysics();
            this.handleMovement();
        }, this.MOVE_THRESHOLD);
    }

    stopMovement() {
        if (this.movementInterval) {
            clearInterval(this.movementInterval);
            this.movementInterval = null;
        }
        
        // Don't immediately stop - let physics handle deceleration
        this.localTank.targetSpeed = 0;
    }

    stopAllMovement() {
        // Emergency stop - clear all movement states
        const movementKeys = ['KeyW', 'KeyA', 'KeyS', 'KeyD'];
        movementKeys.forEach(key => {
            this.keyStates[key] = false;
        });
        this.stopMovement();
        
        // Force stop for emergency
        this.localTank.velocityX = 0;
        this.localTank.velocityY = 0;
        this.localTank.speed = 0;
        this.localTank.targetSpeed = 0;
    }

    updateTankPhysics() {
        if (!this.localTank) return;
        
        // Handle rotation
        let isRotating = false;
        if (this.keyStates['KeyA']) {
            this.localTank.angle -= this.ROTATION_SPEED;
            isRotating = true;
        }
        if (this.keyStates['KeyD']) {
            this.localTank.angle += this.ROTATION_SPEED;
            isRotating = true;
        }
        
        // Normalize angle to 0-2π range
        this.localTank.angle = this.normalizeAngle(this.localTank.angle);
        
        // Handle forward/backward movement
        if (this.keyStates['KeyW']) {
            this.localTank.targetSpeed = this.MAX_SPEED;
        } else if (this.keyStates['KeyS']) {
            this.localTank.targetSpeed = -this.MAX_SPEED * 0.7; // Reverse is slower
        } else {
            this.localTank.targetSpeed = 0;
        }
        
        // Apply acceleration/deceleration
        const speedDiff = this.localTank.targetSpeed - this.localTank.speed;
        this.localTank.speed += speedDiff * this.ACCELERATION;
        
        // Apply friction when not accelerating
        if (Math.abs(this.localTank.targetSpeed) < 0.1) {
            this.localTank.speed *= this.FRICTION;
        }
        
        // Stop very small movements
        if (Math.abs(this.localTank.speed) < this.MIN_SPEED_THRESHOLD) {
            this.localTank.speed = 0;
        }
        
        // Calculate velocity components based on angle and speed
        this.localTank.velocityX = Math.cos(this.localTank.angle) * this.localTank.speed;
        this.localTank.velocityY = Math.sin(this.localTank.angle) * this.localTank.speed;
        
        // Update position
        this.localTank.x += this.localTank.velocityX;
        this.localTank.y += this.localTank.velocityY;
        
        // Boundary checking
        const halfSize = this.TANK_SIZE / 2;
        this.localTank.x = Math.max(halfSize, Math.min(this.CANVAS_WIDTH - halfSize, this.localTank.x));
        this.localTank.y = Math.max(halfSize, Math.min(this.CANVAS_HEIGHT - halfSize, this.localTank.y));
        
        // Update movement state
        this.localTank.isMoving = Math.abs(this.localTank.speed) > this.MIN_SPEED_THRESHOLD || isRotating;
    }

    normalizeAngle(angle) {
        while (angle < 0) angle += Math.PI * 2;
        while (angle >= Math.PI * 2) angle -= Math.PI * 2;
        return angle;
    }

    angleToDirection(angle) {
        // Convert angle to 8-directional system for server compatibility
        const normalizedAngle = this.normalizeAngle(angle);
        const directions = ['RIGHT', 'DOWN', 'LEFT', 'UP'];
        const sectorAngle = Math.PI / 2;
        const sector = Math.round(normalizedAngle / sectorAngle) % 4;
        return directions[sector];
    }

    handleMovement() {
        if (!this.playerTank || !this.stompClient || !this.stompClient.connected) return;
        
        const now = Date.now();
        if (now - this.lastMoveTime < this.MOVE_THRESHOLD) return;
        
        this.lastMoveTime = now;
        
        // Sync local tank with player tank if needed
        if (!this.localTank.initialized && this.playerTank) {
            this.localTank.x = this.playerTank.x;
            this.localTank.y = this.playerTank.y;
            this.localTank.angle = this.directionToAngle(this.playerTank.direction || 'UP');
            this.localTank.initialized = true;
        }
        
        if (this.localTank.isMoving) {
            // Update player tank for immediate feedback
            this.playerTank.x = this.localTank.x;
            this.playerTank.y = this.localTank.y;
            this.playerTank.direction = this.angleToDirection(this.localTank.angle);
            this.playerTank.isMoving = this.localTank.isMoving;
            
            // Send movement to server
            this.sendPlayerAction('PLAYER_MOVE', {
                x: this.localTank.x,
                y: this.localTank.y,
                direction: this.angleToDirection(this.localTank.angle),
                isMoving: this.localTank.isMoving,
                angle: this.localTank.angle // Send angle for better synchronization
            });
        } else if (this.playerTank.isMoving) {
            // Send stop signal
            this.playerTank.isMoving = false;
            this.sendPlayerAction('PLAYER_STOP', {
                x: this.localTank.x,
                y: this.localTank.y,
                direction: this.angleToDirection(this.localTank.angle),
                isMoving: false,
                angle: this.localTank.angle
            });
        }
    }

    directionToAngle(direction) {
        const directionMap = {
            'UP': -Math.PI / 2,
            'DOWN': Math.PI / 2,
            'LEFT': Math.PI,
            'RIGHT': 0
        };
        return directionMap[direction] || 0;
    }

    shoot() {
        if (!this.stompClient || !this.playerTank) return;
        
        const now = Date.now();
        if (now - this.lastShootTime < this.SHOOT_COOLDOWN) return;
        
        if (this.playerTank.ammunition <= 0) {
            this.showMessage("Out of ammo! Reloading...", 1000);
            this.requestReload();
            return;
        }
        
        this.lastShootTime = now;
        this.sendPlayerAction('PLAYER_SHOOT', {});
        
        // Visual feedback
        this.showShootEffect();
    }

    requestReload() {
        if (!this.stompClient) return;
        
        this.sendPlayerAction('PLAYER_RELOAD', {});
        this.showMessage("Reloading...", 1500);
    }

    togglePause() {
        // Toggle pause state
        if (this.isPaused) {
            this.resumeGame();
        } else {
            this.pauseGame();
        }
    }

    pauseGame() {
        this.isPaused = true;
        this.stopAllMovement();
        this.showMessage("Game Paused - Press ESC to resume", 0);
    }

    resumeGame() {
        this.isPaused = false;
        this.hideMessage();
    }

    showShootEffect() {
        // Add visual effect for shooting
        console.log('Shoot effect!');
    }

    showMessage(message, duration = 3000) {
        const messagesContainer = document.getElementById('gameMessages');
        if (!messagesContainer) return;
        
        // Clear existing messages if duration is 0 (permanent message)
        if (duration === 0) {
            messagesContainer.innerHTML = '';
        }
        
        const messageElement = document.createElement('div');
        messageElement.className = 'game-message';
        messageElement.textContent = message;
        
        messagesContainer.appendChild(messageElement);
        
        if (duration > 0) {
            setTimeout(() => {
                if (messageElement.parentNode) {
                    messageElement.parentNode.removeChild(messageElement);
                }
            }, duration);
        }
    }    hideMessage() {
        const messagesContainer = document.getElementById('gameMessages');
        if (messagesContainer) {
            messagesContainer.innerHTML = '';
        }
    }
    
    handleAchievementNotification(achievement) {
        console.log('Achievement notification received:', achievement);
        
        // Show achievement as a special message
        this.showAchievementMessage(
            achievement.achievementName, 
            achievement.description, 
            achievement.bonusPoints
        );
    }
    
    handlePersonalAchievement(achievement) {
        console.log('Personal achievement received:', achievement);
        
        // Show personal achievement with enhanced styling
        this.showPersonalAchievementMessage(
            achievement.achievementName, 
            achievement.description, 
            achievement.bonusPoints
        );
    }
    
    showAchievementMessage(title, description, points) {
        const messagesContainer = document.getElementById('gameMessages');
        if (!messagesContainer) return;
        
        const achievementElement = document.createElement('div');
        achievementElement.className = 'achievement-notification global';
        achievementElement.innerHTML = `
            <div class="achievement-header">🏆 ${title}</div>
            <div class="achievement-description">${description}</div>
            <div class="achievement-points">+${points} points</div>
        `;
        
        messagesContainer.appendChild(achievementElement);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (achievementElement.parentNode) {
                achievementElement.parentNode.removeChild(achievementElement);
            }
        }, 5000);
    }
    
    showPersonalAchievementMessage(title, description, points) {
        const messagesContainer = document.getElementById('gameMessages');
        if (!messagesContainer) return;
        
        const achievementElement = document.createElement('div');
        achievementElement.className = 'achievement-notification personal';
        achievementElement.innerHTML = `
            <div class="achievement-header">🎯 YOUR ACHIEVEMENT</div>
            <div class="achievement-title">${title}</div>
            <div class="achievement-description">${description}</div>
            <div class="achievement-points">+${points} points earned!</div>
        `;
        
        messagesContainer.appendChild(achievementElement);
        
        // Auto-remove after 7 seconds
        setTimeout(() => {
            if (achievementElement.parentNode) {
                achievementElement.parentNode.removeChild(achievementElement);
            }
        }, 7000);
    }

    handleGameStateUpdate(gameState) {
        console.log('Game state updated:', gameState);
        this.gameState = gameState;
        
        // Update our own tank from server state (server reconciliation)
        if (gameState.tanks && this.sessionId) {
            const serverTank = gameState.tanks.find(tank => tank.playerId === this.sessionId);
            if (serverTank) {
                // Only update position if there's significant difference (anti-jitter)
                const threshold = 10; // pixels
                if (this.playerTank && this.localTank.initialized) {
                    const dx = Math.abs(this.localTank.x - serverTank.x);
                    const dy = Math.abs(this.localTank.y - serverTank.y);
                    
                    if (dx > threshold || dy > threshold) {
                        // Server correction
                        this.localTank.x = serverTank.x;
                        this.localTank.y = serverTank.y;
                        this.localTank.angle = this.directionToAngle(serverTank.direction);
                    }
                    
                    // Always update other properties
                    this.playerTank.health = serverTank.health;
                    this.playerTank.ammunition = serverTank.ammunition;
                    this.playerTank.isAlive = serverTank.isAlive;
                } else {
                    this.playerTank = serverTank;
                    if (!this.localTank.initialized) {
                        this.localTank.x = serverTank.x;
                        this.localTank.y = serverTank.y;
                        this.localTank.angle = this.directionToAngle(serverTank.direction);
                        this.localTank.initialized = true;
                    }
                }
            }
        }
        
        // Update UI and render
        this.updateUI();
        this.renderGame();
    }

    handlePlayerUpdate(playerData) {
        console.log('Player update:', playerData);
        // Handle individual player updates
    }

    startGameLoop() {
        const gameLoop = () => {
            if (!this.isPaused) {
                this.updateGame();
                this.renderGame();
            }
            requestAnimationFrame(gameLoop);
        };
        gameLoop();
    }

    updateGame() {
        // Update game logic here
    }

    updateUI() {
        // Обновляем информацию о здоровье, патронах и счете
        if (this.playerTank) {
            const healthText = document.getElementById('healthText');
            const healthBar = document.getElementById('healthBar');
            const ammoText = document.getElementById('ammoText');
            const scoreText = document.getElementById('scoreText');
            
            if (healthText) healthText.textContent = this.playerTank.health || 100;
            if (healthBar) healthBar.style.width = ((this.playerTank.health || 100) / 100 * 100) + '%';
            if (ammoText) ammoText.textContent = this.playerTank.ammunition || 30;
            
            // Обновляем счет из gameState
            if (this.gameState && this.gameState.scores && scoreText) {
                scoreText.textContent = this.gameState.scores[this.sessionId] || 0;
            }
        }
        
        // Обновляем количество игроков
        if (this.gameState && this.gameState.gameInfo) {
            const playerCount = document.getElementById('playerCount');
            if (playerCount) {
                playerCount.textContent = this.gameState.gameInfo.activePlayers || 0;
            }
        }
    }

    renderGame() {
        // Clear canvas
        this.ctx.fillStyle = '#2a2a2a';
        this.ctx.fillRect(0, 0, this.CANVAS_WIDTH, this.CANVAS_HEIGHT);
        
        // Draw grid
        this.drawGrid();
        
        // Draw game objects
        if (this.gameState) {
            this.drawTanks();
            this.drawBullets();
        }
    }

    drawGrid() {
        this.ctx.strokeStyle = '#444444';
        this.ctx.lineWidth = 1;
        
        // Vertical lines
        for (let x = 0; x <= this.CANVAS_WIDTH; x += 20) {
            this.ctx.beginPath();
            this.ctx.moveTo(x, 0);
            this.ctx.lineTo(x, this.CANVAS_HEIGHT);
            this.ctx.stroke();
        }
        
        // Horizontal lines
        for (let y = 0; y <= this.CANVAS_HEIGHT; y += 20) {
            this.ctx.beginPath();
            this.ctx.moveTo(0, y);
            this.ctx.lineTo(this.CANVAS_WIDTH, y);
            this.ctx.stroke();
        }
    }

    drawTanks() {
        // Draw tanks from game state
        if (this.gameState && this.gameState.tanks) {
            this.gameState.tanks.forEach(tank => {
                this.drawTank(tank);
            });
        }
    }

    drawBullets() {
        // Draw bullets from game state
        if (this.gameState && this.gameState.bullets) {
            this.gameState.bullets.forEach(bullet => {
                this.drawBullet(bullet);
            });
        }
    }

    drawTank(tank) {
        const ctx = this.ctx;
        let x, y, angle;
        
        // Use local tank data for our own tank for smooth movement
        if (tank.playerId === this.sessionId && this.localTank.initialized) {
            x = this.localTank.x;
            y = this.localTank.y;
            angle = this.localTank.angle;
        } else {
            x = tank.x;
            y = tank.y;
            angle = this.directionToAngle(tank.direction || 'UP');
        }
        
        const size = this.TANK_SIZE;
        
        ctx.save();
        ctx.translate(x, y);
        ctx.rotate(angle);
        
        // Tank body
        ctx.fillStyle = tank.playerId === this.sessionId ? '#00ff88' : (tank.color || '#ff4444');
        ctx.fillRect(-size/2, -size/2, size, size);
        
        // Tank barrel
        ctx.fillStyle = '#333333';
        const barrelLength = size * 0.8;
        const barrelWidth = 6;
        ctx.fillRect(-barrelWidth/2, -size/2 - barrelLength, barrelWidth, barrelLength);
        
        // Tank direction indicator (small triangle)
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.moveTo(0, -size/2);
        ctx.lineTo(-4, -size/2 + 8);
        ctx.lineTo(4, -size/2 + 8);
        ctx.closePath();
        ctx.fill();
        
        ctx.restore();
        
        // Health bar for other players
        if (tank.playerId !== this.sessionId && tank.health < 100) {
            this.drawHealthBar(x, y - size/2 - 8, tank.health);
        }
        
        // Player name
        ctx.fillStyle = '#ffffff';
        ctx.font = '12px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(tank.playerId.substring(0, 8), x, y + size/2 + 15);
        
        // Speed indicator for debugging (only for our tank)
        if (tank.playerId === this.sessionId && this.localTank.initialized) {
            ctx.fillStyle = '#ffff00';
            ctx.font = '10px Arial';
            ctx.fillText(`Speed: ${this.localTank.speed.toFixed(1)}`, x, y + size/2 + 28);
        }
    }

    drawBullet(bullet) {
        this.ctx.fillStyle = '#ffff00';
        this.ctx.fillRect(
            bullet.x - this.BULLET_SIZE / 2,
            bullet.y - this.BULLET_SIZE / 2,
            this.BULLET_SIZE,
            this.BULLET_SIZE
        );
    }

    drawHealthBar(x, y, health) {
        const width = this.TANK_SIZE;
        const height = 4;
        
        // Background
        this.ctx.fillStyle = '#333333';
        this.ctx.fillRect(x - width/2, y, width, height);
        
        // Health
        this.ctx.fillStyle = health > 50 ? '#00ff00' : health > 25 ? '#ffff00' : '#ff0000';
        this.ctx.fillRect(x - width/2, y, (width * health / 100), height);
    }
}

// НЕ инициализируем игру здесь - это делается в HTML после загрузки всех библиотек
console.log('Tank Game class loaded');
