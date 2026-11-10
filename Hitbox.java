package com.example.testclient.modules;

import com.example.testclient.settings.*;
import com.example.testclient.util.Boxes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class Hitbox extends Module {
    public static Hitbox INSTANCE;
    final NumSetting size = num("Groesse", 0.2, 0.0, 1.0, 0.05);
    final BoolSetting players = bool("Spieler", true);
    final BoolSetting mobs = bool("Mobs", true);
    final BoolSetting show = bool("Anzeigen", true);
    final NumSetting range = num("Anzeige-Range", 32, 8, 96, 4);

    public Hitbox() { super("Hitbox", "Combat", "Trefferboxen vergroessern und anzeigen"); INSTANCE = this; }

    private boolean applies(Entity e) {
        if (e == mc.player) return false;
        if (e instanceof PlayerEntity) return players.value;
        return e instanceof LivingEntity && mobs.value;
    }

    /** Wird vom EntityMixin aufgerufen. */
    public float extra(Entity e) { return isEnabled() && applies(e) ? (float) size.value : 0f; }

    @Override public void onHud(DrawContext ctx) {
        if (!show.value || !inWorld()) return;
        double r2 = range.value * range.value;
        for (Entity e : mc.world.getEntities()) {
            if (!applies(e) || mc.player.squaredDistanceTo(e) > r2) continue;
            Boxes.box2d(ctx, e.getBoundingBox().expand(size.value), 0xFFFFB347, null);
        }
    }
}
