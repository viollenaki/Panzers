# Panzers - 2D Tank Battle Game

Многопользовательская игра-танковый симулятор в реальном времени, построенная с использованием Spring Boot и WebSocket.

## Исправленные проблемы

✅ **Dependency Injection** - Заменена field injection на constructor injection во всех компонентах  
✅ **GameStatistics Entity** - Полностью пересоздана с правильными JPA аннотациями  
✅ **REST API Endpoints** - Добавлены отсутствующие `/api/leaderboard` и `/api/stats` endpoints  
✅ **Code Quality** - Добавлены константы, улучшена обработка ошибок и логирование  
✅ **Configuration** - Очищены настройки базы данных и Spring Security  
✅ **WebSocket Configuration** - Проверена и настроена для реального времени  

## Технологии

- **Backend**: Spring Boot 3.x, Spring Security, Spring WebSocket
- **Database**: MySQL с JPA/Hibernate
- **Frontend**: HTML5 Canvas, JavaScript ES6, CSS3
- **Real-time**: WebSocket с STOMP messaging

## Компоненты игры

### Backend Services
- `UserService` - управление пользователями и аутентификация
- `GameService` - игровая логика и сессии
- `StatisticsService` - статистика игроков и лидерборды
- `CustomUserDetailsService` - Spring Security интеграция

### Game Models
- `User` - модель пользователя с валидацией
- `GameSession` - игровые сессии
- `GameStatistics` - статистика игроков
- `Tank` - игровые объекты танков
- `Bullet` - снаряды

### Controllers
- `AuthController` - регистрация/авторизация
- `GameController` - игровые страницы
- `LeaderboardController` - REST API для статистики
- `GameWebSocketController` - WebSocket endpoints

## Запуск приложения

1. **Убедитесь, что MySQL запущен и создана база данных**
2. **Обновите настройки в `application.properties`**:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/panzers_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Запустите приложение**:
   ```bash
   # Через Maven
   mvn spring-boot:run
   
   # Или через JAR
   java -jar target/Panzers-0.0.1-SNAPSHOT.jar
   ```

4. **Откройте браузер**: http://localhost:8080

## Игровые возможности

- 🎮 **Многопользовательские бои** в реальном времени
- 🏆 **Система рейтингов** с различными категориями
- 📊 **Детальная статистика** игроков
- 🔐 **Регистрация и авторизация** пользователей
- ⚡ **WebSocket соединение** для мгновенных обновлений

## Управление

- **WASD** - движение танка
- **SPACE** - выстрел
- **ESC** - пауза/меню

## API Endpoints

### Authentication
- `GET /login` - страница входа
- `POST /login` - авторизация
- `GET /register` - страница регистрации
- `POST /register` - регистрация нового пользователя

### Game
- `GET /` - главная страница
- `GET /game` - игровая арена
- `GET /leaderboard` - таблица лидеров

### REST API
- `GET /api/leaderboard/{type}` - лидерборды (score/wins/kd/accuracy)
- `GET /api/stats/global` - глобальная статистика

### WebSocket
- `/ws` - основной WebSocket endpoint
- `/topic/game-state` - обновления состояния игры
- `/topic/player-updates` - обновления игроков

## База данных

Автоматически создаются таблицы:
- `users` - пользователи
- `game_statistics` - статистика игроков  
- `game_sessions` - игровые сессии

## Статус проекта

✅ **Готов к запуску** - Все основные баги исправлены  
✅ **Полная компиляция** - JAR файл собирается без ошибок  
✅ **Все компоненты** - Backend, Frontend, WebSocket настроены  

---
*Приложение успешно исправлено и готово к использованию!*
