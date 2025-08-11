package com.evg.playertracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PlayerTrackerHUD {
    private static final int HUD_WIDTH = 200;
    private static final int HUD_HEIGHT = 20;
    private static final int PADDING = 5;
    private static final int BACKGROUND_COLOR = 0x80000000; // Полупрозрачный черный
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    
    // Простой метод для отображения HUD
    public static void renderHUD() {
        if (!Config.HUD_ENABLED.get() || !Config.MOD_ENABLED.get()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return; // Не показываем HUD когда открыт любой экран
        }
        
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            return;
        }
        
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        if (cache == null) {
            return;
        }
        
        List<PlayerData> visiblePlayers = cache.getVisiblePlayers(
            localPlayer.position(),
            Config.MAX_DETECTION_DISTANCE.get(),
            Config.SORT_MODE.get()
        );
        
        if (visiblePlayers.isEmpty()) {
            return; // Не показываем HUD если нет игроков
        }
        
        // Ограничиваем количество отображаемых игроков
        int maxPlayers = Math.min(visiblePlayers.size(), Config.MAX_PLAYERS_DISPLAY.get());
        visiblePlayers = visiblePlayers.subList(0, maxPlayers);
        
        // Рендерим HUD (упрощенная версия)
        renderSimpleHUD(minecraft, visiblePlayers);
    }
    
    private static void renderSimpleHUD(Minecraft minecraft, List<PlayerData> players) {
        // Простая реализация HUD через логирование
        if (minecraft.player != null && players.size() > 0) {
            PlayerTrackerMod.LOGGER.debug("HUD: Обнаружено {} игроков", players.size());
            for (PlayerData player : players) {
                PlayerTrackerMod.LOGGER.debug("Игрок: {} на расстоянии {:.1f} блоков", 
                    player.getUsername(), player.getDistance());
            }
        }
    }
    
    // Метод для переключения режимов HUD
    public static void setHUDMode(Config.HUDMode mode) {
        // Здесь должна быть логика изменения режима HUD
        // В реальной реализации это должно обновлять конфигурацию
        PlayerTrackerMod.LOGGER.info("HUD режим изменен на: {}", mode);
    }
    
    // Метод для переключения HUD
    public static void toggleHUD() {
        // Здесь должна быть логика переключения HUD
        // В реальной реализации это должно обновлять конфигурацию
        PlayerTrackerMod.LOGGER.info("HUD переключен");
    }
    
    // Метод для проверки видимости игрока в FOV
    public static boolean isPlayerInFOV(Vec3 playerPos, Vec3 targetPos, Vec3 playerLookDir) {
        if (!Config.FILTER_NPCS.get()) {
            return true;
        }
        
        Vec3 directionToTarget = targetPos.subtract(playerPos).normalize();
        double angle = Math.acos(playerLookDir.dot(directionToTarget)) * 180 / Math.PI;
        
        return angle <= Config.FOV_FILTER.get() / 2.0;
    }
    
    // Метод для фильтрации NPC
    public static boolean isValidPlayer(String username) {
        if (!Config.FILTER_NPCS.get()) {
            return true;
        }
        
        // Простые правила для фильтрации NPC
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        // Фильтруем слишком короткие имена
        if (username.length() < 3) {
            return false;
        }
        
        // Фильтруем имена с необычными символами
        if (username.matches(".*[^a-zA-Z0-9_].*")) {
            return false;
        }
        
        // Фильтруем имена, которые выглядят как боты
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
}
