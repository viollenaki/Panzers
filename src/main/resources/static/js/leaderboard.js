// Leaderboard functionality
class LeaderboardManager {
    constructor() {
        this.currentTab = 'score';
        this.leaderboardData = {};
        this.refreshInterval = null;
        
        this.initializeLeaderboard();
    }
    
    initializeLeaderboard() {
        this.setupTabHandlers();
        this.loadAllLeaderboards();
        this.startAutoRefresh();
    }
    
    setupTabHandlers() {
        const tabButtons = document.querySelectorAll('.tab-button');
        
        tabButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const tabName = e.target.getAttribute('data-tab');
                this.switchTab(tabName);
            });
        });
    }
    
    switchTab(tabName) {
        // Update active tab button
        document.querySelectorAll('.tab-button').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        
        // Update active tab content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tabName}-tab`).classList.add('active');
        
        this.currentTab = tabName;
        
        // Load data if not already loaded
        if (!this.leaderboardData[tabName]) {
            this.loadLeaderboard(tabName);
        }
    }
    
    async loadAllLeaderboards() {
        const tabs = ['score', 'wins', 'kd', 'accuracy'];
        
        for (const tab of tabs) {
            await this.loadLeaderboard(tab);
        }
        
        this.loadGlobalStats();
    }
    
    async loadLeaderboard(type) {
        try {
            const response = await fetch(`/api/leaderboard/${type}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const data = await response.json();
            this.leaderboardData[type] = data;
            this.renderLeaderboard(type, data);
            
        } catch (error) {
            console.error(`Error loading ${type} leaderboard:`, error);
            this.showErrorMessage(type);
        }
    }
    
    renderLeaderboard(type, data) {
        const container = document.getElementById(`${type}Leaderboard`);
        if (!container) return;
        
        if (!data || data.length === 0) {
            container.innerHTML = '<div class="no-data">No data available yet</div>';
            return;
        }
        
        container.innerHTML = '';
        
        data.forEach((player, index) => {
            const item = this.createLeaderboardItem(player, index + 1, type);
            container.appendChild(item);
        });
    }
    
    createLeaderboardItem(player, rank, type) {
        const item = document.createElement('div');
        item.className = 'leaderboard-item';
        
        // Add special styling for top 3
        let rankClass = '';
        if (rank === 1) rankClass = 'first';
        else if (rank === 2) rankClass = 'second';
        else if (rank === 3) rankClass = 'third';
        
        // Generate avatar from username
        const avatarLetter = player.username ? player.username.charAt(0).toUpperCase() : 'P';
        
        // Get main and sub stats based on type
        const { mainStat, subStat } = this.getStatsForType(player, type);
        
        item.innerHTML = `
            <div class="rank ${rankClass}">#${rank}</div>
            <div class="player-info">
                <div class="player-avatar">${avatarLetter}</div>
                <div class="player-name">${player.username || 'Anonymous'}</div>
            </div>
            <div class="main-stat">${mainStat}</div>
            <div class="sub-stat">${subStat}</div>
        `;
        
        return item;
    }
    
    getStatsForType(player, type) {
        const stats = player.gameStatistics || {};
        
        switch (type) {
            case 'score':
                return {
                    mainStat: this.formatNumber(stats.totalScore || 0),
                    subStat: `${stats.totalGames || 0} games`
                };
            case 'wins':
                const winRate = stats.totalGames > 0 ? 
                    ((stats.wins || 0) / stats.totalGames * 100).toFixed(1) : '0.0';
                return {
                    mainStat: stats.wins || 0,
                    subStat: `${winRate}% rate`
                };
            case 'kd':
                const kdRatio = stats.deaths > 0 ? 
                    ((stats.kills || 0) / stats.deaths).toFixed(2) : (stats.kills || 0);
                return {
                    mainStat: kdRatio,
                    subStat: `${stats.kills || 0}/${stats.deaths || 0}`
                };
            case 'accuracy':
                const accuracy = stats.shotsFired > 0 ? 
                    ((stats.shotsHit || 0) / stats.shotsFired * 100).toFixed(1) : '0.0';
                return {
                    mainStat: `${accuracy}%`,
                    subStat: `${stats.shotsHit || 0}/${stats.shotsFired || 0}`
                };
            default:
                return { mainStat: '-', subStat: '-' };
        }
    }
    
    async loadGlobalStats() {
        try {
            const response = await fetch('/api/stats/global');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const stats = await response.json();
            this.renderGlobalStats(stats);
            
        } catch (error) {
            console.error('Error loading global stats:', error);
            this.renderGlobalStats({});
        }
    }
    
    renderGlobalStats(stats) {
        const elements = {
            totalPlayers: stats.totalPlayers || 0,
            totalGames: stats.totalGames || 0,
            avgScore: stats.averageScore ? Math.round(stats.averageScore) : 0,
            onlinePlayers: stats.onlinePlayers || 0
        };
        
        Object.entries(elements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                this.animateNumber(element, parseInt(element.textContent) || 0, value);
            }
        });
    }
    
    animateNumber(element, from, to) {
        const duration = 1000;
        const steps = 30;
        const stepDuration = duration / steps;
        const increment = (to - from) / steps;
        
        let current = from;
        let step = 0;
        
        const timer = setInterval(() => {
            current += increment;
            step++;
            
            element.textContent = this.formatNumber(Math.round(current));
            
            if (step >= steps) {
                clearInterval(timer);
                element.textContent = this.formatNumber(to);
            }
        }, stepDuration);
    }
    
    formatNumber(num) {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        } else if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toString();
    }
    
    showErrorMessage(type) {
        const container = document.getElementById(`${type}Leaderboard`);
        if (container) {
            container.innerHTML = `
                <div class="no-data">
                    <i>⚠️</i>
                    Unable to load leaderboard data.<br>
                    Please try again later.
                </div>
            `;
        }
    }
    
    startAutoRefresh() {
        // Refresh every 30 seconds
        this.refreshInterval = setInterval(() => {
            this.loadLeaderboard(this.currentTab);
            this.loadGlobalStats();
        }, 30000);
    }
    
    stopAutoRefresh() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
            this.refreshInterval = null;
        }
    }
}

// Initialize leaderboard when page loads
document.addEventListener('DOMContentLoaded', function() {
    const leaderboard = new LeaderboardManager();
    
    // Cleanup on page unload
    window.addEventListener('beforeunload', function() {
        leaderboard.stopAutoRefresh();
    });
    
    // Handle visibility change
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            leaderboard.stopAutoRefresh();
        } else {
            leaderboard.startAutoRefresh();
        }
    });
    
    // Make leaderboard globally accessible for debugging
    window.leaderboardManager = leaderboard;
});
