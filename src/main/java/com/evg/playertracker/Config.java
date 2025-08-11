package com.evg.playertracker;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Основные настройки мода
    public static final ModConfigSpec.BooleanValue MOD_ENABLED = BUILDER
            .comment("Включить/выключить мод")
            .define("modEnabled", true);

    public static final ModConfigSpec.BooleanValue HUD_ENABLED = BUILDER
            .comment("Включить/выключить HUD интерфейс")
            .define("hudEnabled", true);

    public static final ModConfigSpec.IntValue MAX_PLAYERS_DISPLAY = BUILDER
            .comment("Максимальное количество отображаемых игроков")
            .defineInRange("maxPlayersDisplay", 10, 1, 50);

    public static final ModConfigSpec.IntValue MAX_DETECTION_DISTANCE = BUILDER
            .comment("Максимальная дистанция обнаружения игроков (в блоках)")
            .defineInRange("maxDetectionDistance", 100, 10, 1000);

    public static final ModConfigSpec.IntValue UPDATE_INTERVAL = BUILDER
            .comment("Интервал обновления данных (в тиках)")
            .defineInRange("updateInterval", 20, 5, 100);

    public static final ModConfigSpec.BooleanValue FILTER_NPCS = BUILDER
            .comment("Фильтровать NPC (ботов и невалидных никнеймов)")
            .define("filterNPCs", true);

    public static final ModConfigSpec.IntValue FOV_FILTER = BUILDER
            .comment("Фильтр по FOV (угол обзора в градусах)")
            .defineInRange("fovFilter", 90, 30, 180);

    // Настройки кэширования
    public static final ModConfigSpec.IntValue CACHE_LIFETIME = BUILDER
            .comment("Время жизни кэшированных данных (в секундах)")
            .defineInRange("cacheLifetime", 300, 60, 3600);

    // Настройки статистики
    public static final ModConfigSpec.BooleanValue AUTO_SAVE_STATS = BUILDER
            .comment("Автоматическое сохранение статистики")
            .define("autoSaveStats", true);

    public static final ModConfigSpec.IntValue STATS_SAVE_INTERVAL = BUILDER
            .comment("Интервал сохранения статистики (в минутах)")
            .defineInRange("statsSaveInterval", 5, 1, 60);

    public static final ModConfigSpec.IntValue MAX_STATS_FILE_SIZE = BUILDER
            .comment("Максимальный размер файла статистики (в МБ)")
            .defineInRange("maxStatsFileSize", 10, 1, 100);

    public static final ModConfigSpec.BooleanValue AUTO_BACKUP_STATS = BUILDER
            .comment("Автоматическое создание резервных копий статистики")
            .define("autoBackupStats", true);

    // Настройки HUD
    public static final ModConfigSpec.EnumValue<HUDMode> HUD_MODE = BUILDER
            .comment("Режим отображения HUD")
            .defineEnum("hudMode", HUDMode.COMPACT);

    public static final ModConfigSpec.EnumValue<SortMode> SORT_MODE = BUILDER
            .comment("Режим сортировки игроков")
            .defineEnum("sortMode", SortMode.DISTANCE);

    public static final ModConfigSpec.BooleanValue SHOW_COORDINATES = BUILDER
            .comment("Показывать координаты игроков")
            .define("showCoordinates", true);

    public static final ModConfigSpec.BooleanValue SHOW_BIOME = BUILDER
            .comment("Показывать биом игрока")
            .define("showBiome", true);

    public static final ModConfigSpec.BooleanValue SHOW_DIRECTION = BUILDER
            .comment("Показывать направление к игроку")
            .define("showDirection", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public enum HUDMode {
        COMPACT("compact"),
        DETAILED("detailed"),
        MINIMAL("minimal");

        private final String name;

        HUDMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum SortMode {
        DISTANCE("distance"),
        NAME("name"),
        TEAM("team"),
        BIOME("biome");

        private final String name;

        SortMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
