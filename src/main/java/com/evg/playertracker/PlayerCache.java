package com.evg.playertracker;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerCache {
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSeen = new ConcurrentHashMap<>();
    private final int maxCacheSize;
    private final long cacheLifetime;

    public PlayerCache(int maxCacheSize, long cacheLifetime) {
        this.maxCacheSize = maxCacheSize;
        this.cacheLifetime = cacheLifetime;
    }

    public void addPlayer(UUID uuid, String username, Vec3 position, Biome biome, Vec3 playerPos) {
        PlayerData playerData = new PlayerData(uuid, username, position, biome, playerPos);
        players.put(uuid, playerData);
        lastSeen.put(uuid, System.currentTimeMillis());
        
        // Очистка старых записей
        cleanup();
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
        lastSeen.remove(uuid);
    }

    public PlayerData getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public List<PlayerData> getAllPlayers() {
        return new ArrayList<>(players.values());
    }

    public List<PlayerData> getVisiblePlayers(Vec3 playerPos, double maxDistance, Config.SortMode sortMode) {
        long currentTime = System.currentTimeMillis();
        
        List<PlayerData> visiblePlayers = players.values().stream()
            .filter(player -> {
                // Проверка дистанции
                if (player.getDistance() > maxDistance) return false;
                
                // Проверка времени жизни кэша
                Long lastSeenTime = lastSeen.get(player.getUuid());
                if (lastSeenTime == null || (currentTime - lastSeenTime) > cacheLifetime * 1000) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());

        // Сортировка
        switch (sortMode) {
            case DISTANCE:
                visiblePlayers.sort(Comparator.comparingDouble(PlayerData::getDistance));
                break;
            case NAME:
                visiblePlayers.sort(Comparator.comparing(PlayerData::getUsername));
                break;
            case TEAM:
                visiblePlayers.sort(Comparator.comparing(PlayerData::getTeamColor));
                break;
            case BIOME:
                visiblePlayers.sort(Comparator.comparing(p -> p.getBiome().toString()));
                break;
        }

        return visiblePlayers;
    }

    public List<PlayerData> getFilteredPlayers(Vec3 playerPos, double maxDistance, String filterType, String filterValue) {
        List<PlayerData> allPlayers = getVisiblePlayers(playerPos, maxDistance, Config.SortMode.DISTANCE);
        
        switch (filterType.toLowerCase()) {
            case "distance":
                try {
                    double distance = Double.parseDouble(filterValue);
                    return allPlayers.stream()
                        .filter(p -> p.getDistance() <= distance)
                        .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    return allPlayers;
                }
            case "team":
                return allPlayers.stream()
                    .filter(p -> p.getTeamColor().equals(filterValue))
                    .collect(Collectors.toList());
            case "biome":
                return allPlayers.stream()
                    .filter(p -> p.getBiome().toString().toLowerCase().contains(filterValue.toLowerCase()))
                    .collect(Collectors.toList());
            default:
                return allPlayers;
        }
    }

    public void clearCache() {
        players.clear();
        lastSeen.clear();
    }

    public int getCacheSize() {
        return players.size();
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayers", players.size());
        stats.put("maxCacheSize", maxCacheSize);
        stats.put("cacheLifetime", cacheLifetime);
        
        if (!players.isEmpty()) {
            long oldestTimestamp = players.values().stream()
                .mapToLong(PlayerData::getTimestamp)
                .min()
                .orElse(0);
            stats.put("oldestEntry", new Date(oldestTimestamp));
        }
        
        return stats;
    }

    private void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Удаление старых записей
        Iterator<Map.Entry<UUID, Long>> iterator = lastSeen.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if ((currentTime - entry.getValue()) > cacheLifetime * 1000) {
                players.remove(entry.getKey());
                iterator.remove();
            }
        }
        
        // Ограничение размера кэша
        if (players.size() > maxCacheSize) {
            List<Map.Entry<UUID, Long>> sortedEntries = lastSeen.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());
            
            int toRemove = players.size() - maxCacheSize;
            for (int i = 0; i < toRemove && i < sortedEntries.size(); i++) {
                UUID uuid = sortedEntries.get(i).getKey();
                players.remove(uuid);
                lastSeen.remove(uuid);
            }
        }
    }

    public boolean isPlayerCached(UUID uuid) {
        Long lastSeenTime = lastSeen.get(uuid);
        if (lastSeenTime == null) return false;
        return (System.currentTimeMillis() - lastSeenTime) <= cacheLifetime * 1000;
    }
}
