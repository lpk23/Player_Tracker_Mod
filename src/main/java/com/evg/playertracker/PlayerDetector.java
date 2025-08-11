package com.evg.playertracker;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerDetector - работает и на клиенте, и на сервере
 * Обнаружение происходит по запросу через команды
 */
public class PlayerDetector {
    private static final ConcurrentHashMap<UUID, Long> lastDetectionTime = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    /**
     * Основной метод обнаружения игроков - вызывается по команде
     */
    public static void detectPlayers(Level level) {
        if (!Config.MOD_ENABLED.get()) {
            return;
        }
        
        tickCounter++;
        if (tickCounter % Config.UPDATE_INTERVAL.get() != 0) {
            return;
        }
        
        performDetection(level);
    }

    private static void performDetection(Level level) {
        List<? extends Player> worldPlayers = level.players();
        
        for (Player player : worldPlayers) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                continue;
            }
            
            Vec3 playerPos = player.position();
            
            for (Player otherPlayer : worldPlayers) {
                if (otherPlayer.equals(player)) {
                    continue;
                }
                
                if (!(otherPlayer instanceof ServerPlayer otherServerPlayer)) {
                    continue;
                }
                
                // Проверяем валидность игрока
                if (!isValidPlayer(otherServerPlayer.getName().getString())) {
                    continue;
                }
                
                Vec3 targetPos = otherServerPlayer.position();
                double distance = playerPos.distanceTo(targetPos);
                
                // Проверяем дистанцию
                if (distance > Config.MAX_DETECTION_DISTANCE.get()) {
                    continue;
                }
                
                // Проверяем, не слишком ли часто мы обновляем этого игрока
                UUID playerUUID = otherServerPlayer.getUUID();
                long currentTime = System.currentTimeMillis();
                Long lastTime = lastDetectionTime.get(playerUUID);
                
                if (lastTime != null && (currentTime - lastTime) < 1000) {
                    continue;
                }
                
                // Обновляем время последнего обнаружения
                lastDetectionTime.put(playerUUID, currentTime);
                
                // Получаем биом
                Biome biome = level.getBiome(otherServerPlayer.blockPosition()).value();
                
                // Добавляем игрока в кэш для каждого игрока, который его видит
                PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
                if (cache != null) {
                    cache.addPlayer(playerUUID, otherServerPlayer.getName().getString(), targetPos, biome, playerPos);
                    
                    // Записываем в статистику
                    PlayerStats stats = PlayerTrackerMod.getInstance().getPlayerStats();
                    if (stats != null) {
                        long sessionDuration = 1000; // Примерная длительность сессии
                        stats.recordPlayerAppearance(playerUUID, otherServerPlayer.getName().getString(), 
                            otherServerPlayer.blockPosition(), biome, sessionDuration);
                    }
                }
            }
        }
        
        // Очищаем старые записи о времени обнаружения
        cleanupOldDetectionTimes();
    }

    private static void cleanupOldDetectionTimes() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 60000; // 1 минута
        
        lastDetectionTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > maxAge
        );
    }

    // Метод для проверки валидности игрока
    public static boolean isValidPlayer(String username) {
        if (!Config.FILTER_NPCS.get()) {
            return true;
        }
        
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        if (username.length() < 3) {
            return false;
        }
        
        if (username.matches(".*[^a-zA-Z0-9_].*")) {
            return false;
        }
        
        String lowerUsername = username.toLowerCase();
        if (lowerUsername.contains("bot") || 
            lowerUsername.contains("npc") || 
            lowerUsername.contains("admin") ||
            lowerUsername.contains("mod") ||
            lowerUsername.contains("helper")) {
            return false;
        }
        
        return true;
    }

    // Метод для принудительного сканирования игроков
    public static void forceScan(Level level) {
        if (level != null) {
            performDetection(level);
        }
    }

    // Метод для получения списка обнаруженных игроков
    public static List<PlayerData> getDetectedPlayers() {
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        if (cache == null) {
            return List.of();
        }
        
        return cache.getAllPlayers();
    }

    // Метод для проверки, находится ли игрок в зоне видимости
    public static boolean isPlayerVisible(UUID playerUUID) {
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        if (cache == null) {
            return false;
        }
        
        return cache.isPlayerCached(playerUUID);
    }

    // Метод для получения информации об игроке
    public static PlayerData getPlayerData(UUID playerUUID) {
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        if (cache == null) {
            return null;
        }
        
        return cache.getPlayer(playerUUID);
    }

    // Метод для очистки кэша обнаружения
    public static void clearDetectionCache() {
        lastDetectionTime.clear();
        
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        if (cache != null) {
            cache.clearCache();
        }
    }

    // Метод для получения статистики обнаружения
    public static String getDetectionStats() {
        int totalDetected = lastDetectionTime.size();
        int totalCached = 0;
        
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        if (cache == null) {
            return "Кэш не инициализирован";
        }
        
        totalCached = cache.getCacheSize();
        
        return String.format("Обнаружено: %d, В кэше: %d, Тиков: %d", 
            totalDetected, totalCached, tickCounter);
    }
}
