package com.example.testclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Eigene 3D->2D-Projektion fuer HUD-ESP (unabhaengig von der Render-API). */
public final class Proj {
    private Proj() {}

    /** @return {screenX, screenY, depth} oder null wenn hinter der Kamera */
    public static double[] project(Vec3d t) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null) return null;
        Vec3d o = cam.getEyePos();
        double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double rx = -cy, rz = -sy;
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
        double dx = t.x - o.x, dy = t.y - o.y, dz = t.z - o.z;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.05) return null;
        double side = dx * rx + dz * rz;
        double up = dx * ux + dy * uy + dz * uz;
        double tan = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        double nx = side / depth / (tan * ((double) w / h));
        double ny = up / depth / tan;
        return new double[]{(1 + nx) / 2 * w, (1 - ny) / 2 * h, depth};
    }

    /** Horizontaler Winkel zum Ziel relativ zur Blickrichtung (Radiant, 0 = geradeaus, + = rechts). */
    public static double angle(Vec3d t) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null) return 0;
        Vec3d o = cam.getEyePos();
        double yaw = Math.toRadians(cam.getYaw());
        double dx = t.x - o.x, dz = t.z - o.z;
        double forward = dx * -Math.sin(yaw) + dz * Math.cos(yaw);
        double right = dx * -Math.cos(yaw) + dz * -Math.sin(yaw);
        return Math.atan2(right, forward);
    }
}
