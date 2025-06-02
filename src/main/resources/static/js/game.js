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
    }

    onWebSocketConnected() {
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
            'KeyW', 'KeyA', 'KeyS', 'KeyD', // Movement
            'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', // Alternative movement
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
        return ['KeyW', 'KeyA', 'KeyS', 'KeyD', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(keyCode);
    }

    hasActiveMovementKeys() {
        const movementKeys = ['KeyW', 'KeyA', 'KeyS', 'KeyD', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'];
        return movementKeys.some(key => this.keyStates[key]);
    }

    startMovement() {
        if (this.movementInterval) return; // Already moving
        
        this.movementInterval = setInterval(() => {
            this.handleMovement();
        }, this.MOVE_THRESHOLD);
    }

    stopMovement() {
        if (this.movementInterval) {
            clearInterval(this.movementInterval);
            this.movementInterval = null;
        }
        
        // Send stop action to server
        if (this.playerTank) {
            this.sendPlayerAction('PLAYER_STOP', {
                x: this.playerTank.x,
                y: this.playerTank.y,
                direction: this.playerTank.direction,
                isMoving: false
            });
        }
    }

    stopAllMovement() {
        // Emergency stop - clear all movement states
        const movementKeys = ['KeyW', 'KeyA', 'KeyS', 'KeyD', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'];
        movementKeys.forEach(key => {
            this.keyStates[key] = false;
        });
        this.stopMovement();
    }

    handleMovement() {
        if (!this.playerTank || !this.stompClient || !this.stompClient.connected) return;
        
        const now = Date.now();
        if (now - this.lastMoveTime < this.MOVE_THRESHOLD) return;
        
        this.lastMoveTime = now;
        
        let direction = null;
        let isMoving = false;
        let newX = this.playerTank.x;
        let newY = this.playerTank.y;
        const speed = this.playerTank.speed || 2;
        
        // Primary movement keys (WASD)
        if (this.keyStates['KeyW'] || this.keyStates['ArrowUp']) {
            direction = 'UP';
            newY -= speed;
            isMoving = true;
        } else if (this.keyStates['KeyS'] || this.keyStates['ArrowDown']) {
            direction = 'DOWN';
            newY += speed;
            isMoving = true;
        } else if (this.keyStates['KeyA'] || this.keyStates['ArrowLeft']) {
            direction = 'LEFT';
            newX -= speed;
            isMoving = true;
        } else if (this.keyStates['KeyD'] || this.keyStates['ArrowRight']) {
            direction = 'RIGHT';
            newX += speed;
            isMoving = true;
        }
        
        // Boundary checking
        const halfSize = this.TANK_SIZE / 2;
        newX = Math.max(halfSize, Math.min(this.CANVAS_WIDTH - halfSize, newX));
        newY = Math.max(halfSize, Math.min(this.CANVAS_HEIGHT - halfSize, newY));
        
        if (direction && isMoving) {
            // Update local tank position for smooth movement
            this.playerTank.x = newX;
            this.playerTank.y = newY;
            this.playerTank.direction = direction;
            this.playerTank.isMoving = isMoving;
            
            // Send movement to server
            this.sendPlayerAction('PLAYER_MOVE', {
                x: newX,
                y: newY,
                direction: direction,
                isMoving: isMoving
            });
        }
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
    }

    hideMessage() {
        const messagesContainer = document.getElementById('gameMessages');
        if (messagesContainer) {
            messagesContainer.innerHTML = '';
        }
    }

    handleGameStateUpdate(gameState) {
        console.log('Game state updated:', gameState);
        this.gameState = gameState;
        
        // Обновляем информацию о собственном танке
        if (gameState.tanks && this.sessionId) {
            this.playerTank = gameState.tanks.find(tank => tank.playerId === this.sessionId);
        }
        
        // Обновляем UI
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
        this.ctx.fillStyle = tank.color || '#00ff88';
        this.ctx.fillRect(
            tank.x - this.TANK_SIZE / 2,
            tank.y - this.TANK_SIZE / 2,
            this.TANK_SIZE,
            this.TANK_SIZE
        );
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
}

// НЕ инициализируем игру здесь - это делается в HTML после загрузки всех библиотек
console.log('Tank Game class loaded');
