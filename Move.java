package com.example.testclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public final class Move {
    private Move() {}

    public static boolean moving(MinecraftClient mc) {
        GameOptions o = mc.options;
        return o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed();
    }

    /** Einheitsvektor {x, z} der gedrueckten Bewegungstasten relativ zur Blickrichtung. */
    public static double[] dir(MinecraftClient mc) {
        GameOptions o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double l = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && l == 0) return new double[]{0, 0};
        double len = Math.sqrt(f * f + l * l);
        f /= len; l /= len;
        double y = Math.toRadians(mc.player.getYaw());
        return new double[]{-Math.sin(y) * f + Math.cos(y) * l, Math.cos(y) * f + Math.sin(y) * l};
    }
}
