package com.evg.playertracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final String username;
    private final Vec3 position;
    private final Biome biome;
    private final long timestamp;
    private final double distance;
    private final String direction;
    private final String teamColor;

    public PlayerData(UUID uuid, String username, Vec3 position, Biome biome, Vec3 playerPos) {
        this.uuid = uuid;
        this.username = username;
        this.position = position;
        this.biome = biome;
        this.timestamp = System.currentTimeMillis();
        this.distance = position.distanceTo(playerPos);
        this.direction = calculateDirection(playerPos, position);
        this.teamColor = extractTeamColor(username);
    }

    private String calculateDirection(Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double angle = Math.atan2(diff.z, diff.x) * 180 / Math.PI;
        
        if (angle < 0) angle += 360;
        
        if (angle >= 315 || angle < 45) return "→"; // East
        if (angle >= 45 && angle < 135) return "↓"; // South
        if (angle >= 135 && angle < 225) return "←"; // West
        if (angle >= 225 && angle < 315) return "↑"; // North
        
        return "?";
    }

    private String extractTeamColor(String username) {
        // Простая логика для определения цвета команды
        // В реальной реализации можно использовать Team API
        if (username.startsWith("§")) {
            return username.substring(0, 2);
        }
        return "§f"; // Белый цвет по умолчанию
    }

    // Getters
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public Vec3 getPosition() { return position; }
    public Biome getBiome() { return biome; }
    public long getTimestamp() { return timestamp; }
    public double getDistance() { return distance; }
    public String getDirection() { return direction; }
    public String getTeamColor() { return teamColor; }
    public BlockPos getBlockPos() { return new BlockPos((int) position.x, (int) position.y, (int) position.z); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PlayerData that = (PlayerData) obj;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return String.format("PlayerData{username='%s', distance=%.1f, direction='%s', biome='%s'}", 
            username, distance, direction, biome.toString());
    }
}
