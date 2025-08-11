package com.evg.playertracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerDetector - работает на клиенте через зону рендера
 * Обнаружение происходит по запросу через команды
 */
public class PlayerDetector {
    private static final ConcurrentHashMap<UUID, Long> lastDetectionTime = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    /**
     * Основной метод обнаружения игроков - вызывается по команде
     */
    public static void detectPlayers() {
        if (!Config.MOD_ENABLED.get()) {
            return;
        }
        
        tickCounter++;
        if (tickCounter % Config.UPDATE_INTERVAL.get() != 0) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        Level level = minecraft.level;
        
        if (localPlayer == null || level == null) {
            return;
        }
        
        performDetection(localPlayer, level);
    }

    private static void performDetection(LocalPlayer localPlayer, Level level) {
        Vec3 playerPos = localPlayer.position();
        Vec3 playerLookDir = localPlayer.getLookAngle();
        
        // Получаем всех игроков в мире
        List<? extends Player> worldPlayers = level.players();
        
        for (Player player : worldPlayers) {
            // Пропускаем самого игрока
            if (player.equals(localPlayer)) {
                continue;
            }
            
            // Проверяем валидность игрока
            if (!isValidPlayer(player.getName().getString())) {
                continue;
            }
            
            Vec3 targetPos = player.position();
            double distance = playerPos.distanceTo(targetPos);
            
            // Проверяем дистанцию
            if (distance > Config.MAX_DETECTION_DISTANCE.get()) {
                continue;
            }
            
            // Проверяем FOV фильтр
            if (!isPlayerInFOV(playerPos, targetPos, playerLookDir)) {
                continue;
            }
            
            // Проверяем, не слишком ли часто мы обновляем этого игрока
            UUID playerUUID = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastDetectionTime.get(playerUUID);
            
            if (lastTime != null && (currentTime - lastTime) < 1000) {
                continue;
            }
            
            // Обновляем время последнего обнаружения
            lastDetectionTime.put(playerUUID, currentTime);
            
            // Получаем биом
            Biome biome = level.getBiome(player.blockPosition()).value();
            
            // Добавляем игрока в кэш
            PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
            if (cache != null) {
                cache.addPlayer(playerUUID, player.getName().getString(), targetPos, biome, playerPos);
                
                // Записываем в статистику
                PlayerStats stats = PlayerTrackerMod.getInstance().getPlayerStats();
                if (stats != null) {
                    long sessionDuration = 1000; // Примерная длительность сессии
                    stats.recordPlayerAppearance(playerUUID, player.getName().getString(), 
                        player.blockPosition(), biome, sessionDuration);
                }
            }
        }
        
        // Очищаем старые записи о времени обнаружения
        cleanupOldDetectionTimes();
    }

    private static boolean isPlayerInFOV(Vec3 playerPos, Vec3 targetPos, Vec3 playerLookDir) {
        if (!Config.FOV_FILTER.get()) {
            return true;
        }
        
        Vec3 directionToTarget = targetPos.subtract(playerPos).normalize();
        double dotProduct = playerLookDir.dot(directionToTarget);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct)));
        double fovRadians = Math.toRadians(Config.FOV_ANGLE.get());
        
        return angle <= fovRadians / 2.0;
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
    public static void forceScan() {
        detectPlayers();
    }

    // Метод для получения списка обнаруженных игроков
    public static List<PlayerData> getDetectedPlayers() {
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        if (cache == null) {
            return List.of();
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        
        if (localPlayer == null) {
            return List.of();
        }
        
        return cache.getVisiblePlayers(
            localPlayer.position(),
            Config.MAX_DETECTION_DISTANCE.get(),
            Config.SORT_MODE.get()
        );
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
