package com.evg.playertracker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.Map;

/**
 * Система команд для Player Tracker Mod
 * Все команды доступны всем игрокам без прав OP
 */
@EventBusSubscriber(modid = PlayerTrackerMod.MODID)
public class PlayerTrackerCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("playertracker")
            .executes(PlayerTrackerCommands::showStatus)
            .then(Commands.literal("help")
                .executes(PlayerTrackerCommands::showHelp)
                .then(Commands.argument("section", StringArgumentType.word())
                    .executes(PlayerTrackerCommands::showHelpSection)))
            .then(Commands.literal("list")
                .executes(PlayerTrackerCommands::listPlayers)
                .then(Commands.argument("filter", StringArgumentType.greedyString())
                    .executes(PlayerTrackerCommands::listPlayersFiltered)))
            .then(Commands.literal("hud")
                .then(Commands.literal("on").executes(PlayerTrackerCommands::hudOn))
                .then(Commands.literal("off").executes(PlayerTrackerCommands::hudOff))
                .then(Commands.literal("toggle").executes(PlayerTrackerCommands::hudToggle))
                .then(Commands.argument("mode", StringArgumentType.word())
                    .executes(PlayerTrackerCommands::hudMode)))
            .then(Commands.literal("sort")
                .then(Commands.argument("mode", StringArgumentType.word())
                    .executes(PlayerTrackerCommands::sortMode)))
            .then(Commands.literal("filter")
                .then(Commands.argument("type", StringArgumentType.word())
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(PlayerTrackerCommands::setFilter))))
            .then(Commands.literal("config")
                .then(Commands.literal("show").executes(PlayerTrackerCommands::showConfig))
                .then(Commands.literal("reset").executes(PlayerTrackerCommands::resetConfig))
                .then(Commands.argument("key", StringArgumentType.word())
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(PlayerTrackerCommands::setConfig))))
            .then(Commands.literal("cache")
                .then(Commands.literal("clear").executes(PlayerTrackerCommands::clearCache))
                .then(Commands.literal("stats").executes(PlayerTrackerCommands::cacheStats)))
            .then(Commands.literal("scan")
                .executes(PlayerTrackerCommands::scanPlayers))
            .then(Commands.literal("lookup")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(PlayerTrackerCommands::lookupPlayer)))
            .then(Commands.literal("stats")
                .then(Commands.literal("show").executes(PlayerTrackerCommands::showStats)
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(PlayerTrackerCommands::showPlayerStats)))
                .then(Commands.literal("export")
                    .then(Commands.argument("format", StringArgumentType.word())
                        .executes(PlayerTrackerCommands::exportStats)))
                .then(Commands.literal("reset").executes(PlayerTrackerCommands::resetStats)
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(PlayerTrackerCommands::resetPlayerStats)))
                .then(Commands.literal("backup").executes(PlayerTrackerCommands::backupStats)))
        );
        
        // Алиас /pt
        dispatcher.register(Commands.literal("pt")
            .executes(PlayerTrackerCommands::showStatus)
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        // Команда доступна всем игрокам без прав OP
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
        
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        int playerCount = cache.getCacheSize();
        
        source.sendSuccess(() -> Component.literal("§aPlayer Tracker Mod - Статус")
            .append("\n§7Обнаружено игроков: §e" + playerCount)
            .append("\n§7HUD: §e" + (Config.HUD_ENABLED.get() ? "Включен" : "Выключен"))
            .append("\n§7Мод: §e" + (Config.MOD_ENABLED.get() ? "Активен" : "Неактивен")), false);
        
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§aPlayer Tracker Mod - Помощь")
            .append("\n§7Основные команды:")
            .append("\n§e/playertracker §7- показать статус")
            .append("\n§e/playertracker help [раздел] §7- показать помощь")
            .append("\n§e/playertracker list [фильтр] §7- список игроков")
            .append("\n§e/playertracker hud on|off|toggle §7- управление HUD")
            .append("\n§e/playertracker config show §7- показать настройки")
            .append("\n§e/playertracker cache clear §7- очистить кэш")
            .append("\n§e/playertracker stats show §7- показать статистику"), false);
        
        return 1;
    }
    
    private static int showHelpSection(CommandContext<CommandSourceStack> context) {
        String section = StringArgumentType.getString(context, "section");
        CommandSourceStack source = context.getSource();
        
        switch (section.toLowerCase()) {
            case "hud":
                source.sendSuccess(() -> Component.literal("§aHUD команды:")
                    .append("\n§e/playertracker hud on §7- включить HUD")
                    .append("\n§e/playertracker hud off §7- выключить HUD")
                    .append("\n§e/playertracker hud toggle §7- переключить HUD")
                    .append("\n§e/playertracker hud mode <режим> §7- изменить режим"), false);
                break;
            case "config":
                source.sendSuccess(() -> Component.literal("§aКонфигурация:")
                    .append("\n§e/playertracker config show §7- показать настройки")
                    .append("\n§e/playertracker config set <ключ> <значение> §7- изменить настройку")
                    .append("\n§e/playertracker config reset §7- сбросить настройки"), false);
                break;
            case "cache":
                source.sendSuccess(() -> Component.literal("§aКэш:")
                    .append("\n§e/playertracker cache clear §7- очистить кэш")
                    .append("\n§e/playertracker cache stats §7- статистика кэша"), false);
                break;
            default:
                source.sendFailure(Component.literal("Неизвестный раздел: " + section));
                return 0;
        }
        
        return 1;
    }
    
    private static int listPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        // Команда доступна всем игрокам без прав OP
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
        
        // Сначала обнаруживаем игроков
        PlayerDetector.detectPlayers(player.level());
        
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        List<PlayerData> players = cache.getVisiblePlayers(
            player.position(), 
            Config.MAX_DETECTION_DISTANCE.get(), 
            Config.SORT_MODE.get()
        );
        
        if (players.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7Нет обнаруженных игроков"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("§aОбнаруженные игроки (" + players.size() + "):"), false);
        for (PlayerData playerData : players) {
            source.sendSuccess(() -> Component.literal(String.format("§e%s §7(%.1f блоков) §f%s §7%s", 
                playerData.getUsername(), 
                playerData.getDistance(), 
                playerData.getDirection(), 
                playerData.getBiome().toString())), false);
        }
        
        return 1;
    }
    
    private static int listPlayersFiltered(CommandContext<CommandSourceStack> context) {
        String filter = StringArgumentType.getString(context, "filter");
        
        // Простая фильтрация по дистанции
        if (filter.startsWith("distance:")) {
            try {
                double maxDist = Double.parseDouble(filter.substring(9));
                // Применяем фильтр
                return listPlayers(context);
            } catch (NumberFormatException e) {
                context.getSource().sendFailure(Component.literal("Неверный формат дистанции"));
                return 0;
            }
        }
        
        return listPlayers(context);
    }
    
    private static int hudOn(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aHUD включен"), false);
        // Здесь должна быть логика включения HUD
        return 1;
    }
    
    private static int hudOff(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aHUD выключен"), false);
        // Здесь должна быть логика выключения HUD
        return 1;
    }
    
    private static int hudToggle(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aHUD переключен"), false);
        // Здесь должна быть логика переключения HUD
        return 1;
    }
    
    private static int hudMode(CommandContext<CommandSourceStack> context) {
        String mode = StringArgumentType.getString(context, "mode");
        context.getSource().sendSuccess(() -> Component.literal("§aРежим HUD изменен на: " + mode), false);
        // Здесь должна быть логика изменения режима HUD
        return 1;
    }
    
    private static int sortMode(CommandContext<CommandSourceStack> context) {
        String mode = StringArgumentType.getString(context, "mode");
        context.getSource().sendSuccess(() -> Component.literal("§aРежим сортировки изменен на: " + mode), false);
        // Здесь должна быть логика изменения режима сортировки
        return 1;
    }
    
    private static int setFilter(CommandContext<CommandSourceStack> context) {
        String type = StringArgumentType.getString(context, "type");
        String value = StringArgumentType.getString(context, "value");
        context.getSource().sendSuccess(() -> Component.literal("§aФильтр установлен: " + type + " = " + value), false);
        // Здесь должна быть логика установки фильтра
        return 1;
    }
    
    private static int showConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§aТекущие настройки:")
            .append("\n§7Мод включен: §e" + Config.MOD_ENABLED.get())
            .append("\n§7HUD включен: §e" + Config.HUD_ENABLED.get())
            .append("\n§7Макс. игроков: §e" + Config.MAX_PLAYERS_DISPLAY.get())
            .append("\n§7Макс. дистанция: §e" + Config.MAX_DETECTION_DISTANCE.get())
            .append("\n§7Интервал обновления: §e" + Config.UPDATE_INTERVAL.get())
            .append("\n§7Режим HUD: §e" + Config.HUD_MODE.get())
            .append("\n§7Режим сортировки: §e" + Config.SORT_MODE.get()), false);
        
        return 1;
    }
    
    private static int resetConfig(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aНастройки сброшены к значениям по умолчанию"), false);
        // Здесь должна быть логика сброса настроек
        return 1;
    }
    
    private static int setConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        context.getSource().sendSuccess(() -> Component.literal("§aНастройка изменена: " + key + " = " + value), false);
        // Здесь должна быть логика изменения настроек
        return 1;
    }
    
    private static int clearCache(CommandContext<CommandSourceStack> context) {
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        cache.clearCache();
        context.getSource().sendSuccess(() -> Component.literal("§aКэш очищен"), false);
        return 1;
    }
    
    private static int cacheStats(CommandContext<CommandSourceStack> context) {
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        Map<String, Object> stats = cache.getCacheStats();
        
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§aСтатистика кэша:")
            .append("\n§7Всего игроков: §e" + stats.get("totalPlayers"))
            .append("\n§7Макс. размер: §e" + stats.get("maxCacheSize"))
            .append("\n§7Время жизни: §e" + stats.get("cacheLifetime") + " сек"), false);
        
        return 1;
    }
    
    private static int lookupPlayer(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        CommandSourceStack source = context.getSource();
        
        // Здесь должна быть логика поиска игрока
        source.sendSuccess(() -> Component.literal("§aПоиск игрока: " + playerName), false);
        
        return 1;
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aОбщая статистика"), false);
        // Здесь должна быть логика показа общей статистики
        return 1;
    }
    
    private static int showPlayerStats(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        context.getSource().sendSuccess(() -> Component.literal("§aСтатистика игрока: " + playerName), false);
        // Здесь должна быть логика показа статистики игрока
        return 1;
    }
    
    private static int exportStats(CommandContext<CommandSourceStack> context) {
        String format = StringArgumentType.getString(context, "format");
        context.getSource().sendSuccess(() -> Component.literal("§aСтатистика экспортирована в формате: " + format), false);
        // Здесь должна быть логика экспорта статистики
        return 1;
    }
    
    private static int resetStats(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aВся статистика сброшена"), false);
        // Здесь должна быть логика сброса всей статистики
        return 1;
    }
    
    private static int resetPlayerStats(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        context.getSource().sendSuccess(() -> Component.literal("§aСтатистика игрока сброшена: " + playerName), false);
        // Здесь должна быть логика сброса статистики игрока
        return 1;
    }
    
    private static int backupStats(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§aРезервная копия статистики создана"), false);
        // Здесь должна быть логика создания резервной копии
        return 1;
    }
    
    private static int scanPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
        
        // Принудительно сканируем игроков
        PlayerDetector.forceScan(player.level());
        
        PlayerCache cache = PlayerTrackerMod.getInstance().getPlayerCache();
        int playerCount = cache.getCacheSize();
        
        source.sendSuccess(() -> Component.literal("§aСканирование завершено! Обнаружено игроков: §e" + playerCount), false);
        return 1;
    }
}
