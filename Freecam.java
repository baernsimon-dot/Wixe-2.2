package com.example.testclient.modules;

import com.example.testclient.settings.NumSetting;
import com.example.testclient.util.Move;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Kamera loest sich vom Spieler. Der Spieler bleibt stehen, Bewegungspakete werden unterdrueckt. */
public final class Freecam extends Module {
    public static Freecam INSTANCE;
    final NumSetting speed = num("Speed", 1.0, 0.1, 6, 0.1);
    private OtherClientPlayerEntity cam;
    private Vec3d saved;
    private double cx, cy, cz;

    public Freecam() { super("Freecam", "World", "Kamera unabhaengig bewegen"); INSTANCE = this; }

    @Override public void onEnable() {
        if (!inWorld()) { return; }
        ClientPlayerEntity p = mc.player;
        cam = new OtherClientPlayerEntity(mc.world, p.getGameProfile());
        cam.copyPositionAndRotation(p);
        saved = p.getPos();
        cx = p.getX(); cy = p.getY(); cz = p.getZ();
        mc.setCameraEntity(cam);
    }

    @Override public void onTick() {
        if (!inWorld() || cam == null) return;
        ClientPlayerEntity p = mc.player;
        double s = speed.value * 0.5;
        double[] d = Move.dir(mc);
        double vy = 0;
        if (mc.options.jumpKey.isPressed()) vy += s;
        if (mc.options.sneakKey.isPressed()) vy -= s;
        cx += d[0] * s; cy += vy; cz += d[1] * s;
        cam.updatePositionAndAngles(cx, cy, cz, p.getYaw(), p.getPitch());
        p.setPosition(saved);
        p.setVelocity(Vec3d.ZERO);
    }

    @Override public void onDisable() {
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
    }
}
