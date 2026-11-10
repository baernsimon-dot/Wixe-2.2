package com.example.testclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Targets {
    private Targets() {}

    public static boolean valid(MinecraftClient mc, Entity e, boolean players, boolean mobs) {
        if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive() || e instanceof ArmorStandEntity) return false;
        return (e instanceof PlayerEntity && players) || (e instanceof Monster && mobs);
    }

    public static List<LivingEntity> find(MinecraftClient mc, double range, boolean players, boolean mobs) {
        List<LivingEntity> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;
        double r2 = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (!valid(mc, e, players, mobs)) continue;
            if (mc.player.squaredDistanceTo(e) > r2) continue;
            out.add((LivingEntity) e);
        }
        out.sort(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)));
        return out;
    }
}
