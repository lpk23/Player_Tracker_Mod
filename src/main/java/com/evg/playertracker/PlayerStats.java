package com.evg.playertracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStats {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STATS_FILE = "player_stats.json";
    private static final String BACKUP_DIR = "backups";
    
    private final Map<UUID, PlayerStatsData> playerStats = new ConcurrentHashMap<>();
    private String serverName;
    private long lastUpdated;
    private int totalUniquePlayers;
    
    public PlayerStats(String serverName) {
        this.serverName = serverName;
        this.lastUpdated = System.currentTimeMillis();
        this.totalUniquePlayers = 0;
        loadStats();
    }
    
    public void recordPlayerAppearance(UUID uuid, String username, BlockPos position, Biome biome, long sessionDuration) {
        PlayerStatsData stats = playerStats.computeIfAbsent(uuid, k -> {
            totalUniquePlayers++;
            return new PlayerStatsData(username);
        });
        
        stats.recordAppearance(position, biome, sessionDuration);
        lastUpdated = System.currentTimeMillis();
        
        // Автосохранение если включено
        if (Config.AUTO_SAVE_STATS.get()) {
            scheduleAutoSave();
        }
    }
    
    public PlayerStatsData getPlayerStats(UUID uuid) {
        return playerStats.get(uuid);
    }
    
    public Map<UUID, PlayerStatsData> getAllPlayerStats() {
        return new HashMap<>(playerStats);
    }
    
    public int getTotalUniquePlayers() {
        return totalUniquePlayers;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void exportStats(String format) throws IOException {
        switch (format.toLowerCase()) {
            case "json":
                exportToJSON();
                break;
            case "csv":
                exportToCSV();
                break;
            default:
                throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
        }
    }
    
    public void resetStats() {
        playerStats.clear();
        totalUniquePlayers = 0;
        lastUpdated = System.currentTimeMillis();
        try {
            saveStats();
        } catch (IOException e) {
            PlayerTrackerMod.LOGGER.error("Ошибка сохранения статистики при сбросе", e);
        }
    }
    
    public void resetPlayerStats(UUID uuid) {
        PlayerStatsData removed = playerStats.remove(uuid);
        if (removed != null) {
            totalUniquePlayers--;
            lastUpdated = System.currentTimeMillis();
            try {
                saveStats();
            } catch (IOException e) {
                PlayerTrackerMod.LOGGER.error("Ошибка сохранения статистики при сбросе игрока", e);
            }
        }
    }
    
    public void createBackup() throws IOException {
        Path backupDir = Paths.get(BACKUP_DIR);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String backupFileName = String.format("player_stats_backup_%s.json", timestamp);
        Path backupPath = backupDir.resolve(backupFileName);
        
        // Создаем копию текущего файла
        Path statsPath = Paths.get(STATS_FILE);
        if (Files.exists(statsPath)) {
            Files.copy(statsPath, backupPath);
        }
        
        // Очищаем старые бэкапы если превышен лимит
        cleanupOldBackups();
    }
    
    private void scheduleAutoSave() {
        // Простая реализация автосохранения
        // В реальном приложении здесь должен быть планировщик задач
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    saveStats();
                } catch (IOException e) {
                    PlayerTrackerMod.LOGGER.error("Ошибка автосохранения статистики", e);
                }
            }
        }, Config.STATS_SAVE_INTERVAL.get() * 60 * 1000L);
    }
    
    private void saveStats() throws IOException {
        StatsContainer container = new StatsContainer();
        container.serverInfo = new ServerInfo(serverName, lastUpdated, totalUniquePlayers);
        container.players = playerStats;
        
        String json = GSON.toJson(container);
        
        // Проверяем размер файла
        Path statsPath = Paths.get(STATS_FILE);
        if (Files.exists(statsPath) && Files.size(statsPath) > Config.MAX_STATS_FILE_SIZE.get() * 1024 * 1024) {
            // Создаем бэкап перед очисткой
            if (Config.AUTO_BACKUP_STATS.get()) {
                createBackup();
            }
            
            // Очищаем старые записи
            cleanupOldEntries();
        }
        
        try (Writer writer = new FileWriter(STATS_FILE)) {
            writer.write(json);
        }
    }
    
    private void loadStats() {
        Path statsPath = Paths.get(STATS_FILE);
        if (!Files.exists(statsPath)) {
            return;
        }
        
        try (Reader reader = new FileReader(STATS_FILE)) {
            StatsContainer container = GSON.fromJson(reader, StatsContainer.class);
            if (container != null) {
                this.serverName = container.serverInfo.serverName;
                this.lastUpdated = container.serverInfo.lastUpdated;
                this.totalUniquePlayers = container.serverInfo.totalUniquePlayers;
                if (container.players != null) {
                    this.playerStats.putAll(container.players);
                }
            }
        } catch (IOException e) {
            PlayerTrackerMod.LOGGER.error("Ошибка загрузки статистики", e);
        }
    }
    
    private void exportToJSON() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String exportFileName = String.format("player_stats_export_%s.json", timestamp);
        
        StatsContainer container = new StatsContainer();
        container.serverInfo = new ServerInfo(serverName, lastUpdated, totalUniquePlayers);
        container.players = playerStats;
        
        String json = GSON.toJson(container);
        
        try (Writer writer = new FileWriter(exportFileName)) {
            writer.write(json);
        }
    }
    
    private void exportToCSV() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String exportFileName = String.format("player_stats_export_%s.csv", timestamp);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(exportFileName))) {
            // Заголовок
            writer.println("UUID,Username,First Seen,Last Seen,Total Sessions,Total Online Time,Appearances Count");
            
            // Данные
            for (Map.Entry<UUID, PlayerStatsData> entry : playerStats.entrySet()) {
                PlayerStatsData stats = entry.getValue();
                writer.printf("%s,%s,%s,%s,%d,%d,%d%n",
                    entry.getKey(),
                    stats.username,
                    formatTimestamp(stats.firstSeen),
                    formatTimestamp(stats.lastSeen),
                    stats.totalSessions,
                    stats.totalOnlineTime,
                    stats.appearances.size()
                );
            }
        }
    }
    
    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private void cleanupOldEntries() {
        // Удаляем самые старые записи, оставляя только последние 1000
        if (playerStats.size() > 1000) {
            List<Map.Entry<UUID, PlayerStatsData>> sortedEntries = playerStats.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().lastSeen))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            int toRemove = playerStats.size() - 1000;
            for (int i = 0; i < toRemove && i < sortedEntries.size(); i++) {
                playerStats.remove(sortedEntries.get(i).getKey());
            }
            
            totalUniquePlayers = playerStats.size();
        }
    }
    
    private void cleanupOldBackups() {
        try {
            Path backupDir = Paths.get(BACKUP_DIR);
            if (!Files.exists(backupDir)) return;
            
            List<Path> backupFiles = Files.list(backupDir)
                .filter(path -> path.toString().endsWith(".json"))
                .sorted(Comparator.comparingLong(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            // Оставляем только последние 10 бэкапов
            if (backupFiles.size() > 10) {
                for (int i = 0; i < backupFiles.size() - 10; i++) {
                    Files.deleteIfExists(backupFiles.get(i));
                }
            }
        } catch (IOException e) {
            PlayerTrackerMod.LOGGER.error("Ошибка очистки старых бэкапов", e);
        }
    }
    
    // Внутренние классы для JSON сериализации
    public static class StatsContainer {
        public ServerInfo serverInfo;
        public Map<UUID, PlayerStatsData> players;
    }
    
    public static class ServerInfo {
        public String serverName;
        public long lastUpdated;
        public int totalUniquePlayers;
        
        public ServerInfo(String serverName, long lastUpdated, int totalUniquePlayers) {
            this.serverName = serverName;
            this.lastUpdated = lastUpdated;
            this.totalUniquePlayers = totalUniquePlayers;
        }
    }
    
    public static class PlayerStatsData {
        public String username;
        public long firstSeen;
        public long lastSeen;
        public int totalSessions;
        public long totalOnlineTime;
        public List<AppearanceData> appearances;
        
        public PlayerStatsData(String username) {
            this.username = username;
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = System.currentTimeMillis();
            this.totalSessions = 0;
            this.totalOnlineTime = 0;
            this.appearances = new ArrayList<>();
        }
        
        public void recordAppearance(BlockPos position, Biome biome, long sessionDuration) {
            this.lastSeen = System.currentTimeMillis();
            this.totalSessions++;
            this.totalOnlineTime += sessionDuration;
            
            AppearanceData appearance = new AppearanceData(position, biome, sessionDuration);
            this.appearances.add(appearance);
        }
    }
    
    public static class AppearanceData {
        public int x, y, z;
        public String biome;
        public long timestamp;
        public long sessionDuration;
        
        public AppearanceData(BlockPos position, Biome biome, long sessionDuration) {
            this.x = position.getX();
            this.y = position.getY();
            this.z = position.getZ();
            this.biome = biome.toString();
            this.timestamp = System.currentTimeMillis();
            this.sessionDuration = sessionDuration;
        }
    }
}
