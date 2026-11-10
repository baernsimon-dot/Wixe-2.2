package com.example.testclient.modules;

import com.example.testclient.settings.*;
import com.example.testclient.util.Rot;
import com.example.testclient.util.Targets;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Random;

final class KillAura extends Module {
    final NumSetting range = num("Range", 4.0, 1.0, 8.0, 0.1);
    final NumSetting cps = num("CPS", 10, 1, 20, 1);
    final BoolSetting cooldown = bool("Cooldown", true);
    final BoolSetting rotate = bool("Rotate", false);
    final BoolSetting multi = bool("Multi", false);
    final BoolSetting players = bool("Spieler", true);
    final BoolSetting mobs = bool("Monster", true);
    private int ticks;

    KillAura() { super("KillAura", "Combat", "Greift Ziele in Reichweite an"); }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null) return;
        ClientPlayerEntity p = mc.player;
        ticks++;
        if (cooldown.value) { if (p.getAttackCooldownProgress(0.5f) < 0.95f) return; }
        else if (ticks < 20.0 / cps.value) return;
        List<LivingEntity> t = Targets.find(mc, range.value, players.value, mobs.value);
        if (t.isEmpty()) return;
        ticks = 0;
        int n = multi.value ? Math.min(t.size(), 4) : 1;
        for (int i = 0; i < n; i++) {
            LivingEntity e = t.get(i);
            if (i == 0 && rotate.value) {
                float[] r = Rot.to(p.getEyePos(), e.getBoundingBox().getCenter());
                p.setYaw(r[0]); p.setPitch(r[1]);
            }
            mc.interactionManager.attackEntity(p, e);
        }
        p.swingHand(Hand.MAIN_HAND);
    }
}

final class AimAssist extends Module {
    final NumSetting range = num("Range", 5.0, 2.0, 8.0, 0.5);
    final NumSetting fov = num("FOV", 90, 10, 360, 5);
    final NumSetting speed = num("Staerke", 0.25, 0.05, 1.0, 0.05);
    final BoolSetting onlyAttack = bool("Nur beim Angreifen", true);
    final BoolSetting players = bool("Spieler", true);
    final BoolSetting mobs = bool("Monster", true);

    AimAssist() { super("AimAssist", "Combat", "Unterstuetzung beim Zielen"); }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null) return;
        if (onlyAttack.value && !mc.options.attackKey.isPressed()) return;
        ClientPlayerEntity p = mc.player;
        double bestAng = Double.MAX_VALUE; float bestYaw = 0, bestPitch = 0;
        for (LivingEntity e : Targets.find(mc, range.value, players.value, mobs.value)) {
            float[] r = Rot.to(p.getEyePos(), e.getBoundingBox().getCenter());
            float dy = MathHelper.wrapDegrees(r[0] - p.getYaw()), dp = r[1] - p.getPitch();
            double ang = Math.hypot(dy, dp);
            if (ang > fov.value / 2.0 || ang >= bestAng) continue;
            bestAng = ang; bestYaw = dy; bestPitch = dp;
        }
        if (bestAng == Double.MAX_VALUE) return;
        p.setYaw(p.getYaw() + (float) (bestYaw * speed.value));
        p.setPitch(MathHelper.clamp(p.getPitch() + (float) (bestPitch * speed.value), -90f, 90f));
    }
}

final class TriggerBot extends Module {
    final BoolSetting players = bool("Spieler", true);
    final BoolSetting mobs = bool("Monster", true);

    TriggerBot() { super("TriggerBot", "Combat", "Greift automatisch an, wenn ein Ziel im Fadenkreuz ist"); }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null) return;
        ClientPlayerEntity p = mc.player;
        if (mc.crosshairTarget instanceof EntityHitResult h
                && Targets.valid(mc, h.getEntity(), players.value, mobs.value)
                && p.getAttackCooldownProgress(0.5f) >= 0.95f) {
            mc.interactionManager.attackEntity(p, h.getEntity());
            p.swingHand(Hand.MAIN_HAND);
        }
    }
}

final class Reach extends AttrModule {
    final NumSetting entity = num("Angriff", 4.5, 3.0, 8.0, 0.1);
    final NumSetting block = num("Bloecke", 5.0, 4.5, 8.0, 0.1);

    Reach() { super("Reach", "Combat", "Erhoehte Reichweite"); }

    @Override public void onTick() {
        if (!inWorld()) return;
        apply(EntityAttributes.ENTITY_INTERACTION_RANGE, "reach_entity", entity.value - 3.0);
        apply(EntityAttributes.BLOCK_INTERACTION_RANGE, "reach_block", block.value - 4.5);
    }
    @Override public void onDisable() {
        clear(EntityAttributes.ENTITY_INTERACTION_RANGE, "reach_entity");
        clear(EntityAttributes.BLOCK_INTERACTION_RANGE, "reach_block");
    }
}

final class Criticals extends Module {
    Criticals() {
        super("Criticals", "Combat", "Kritische Treffer automatisieren");
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (isEnabled() && world.isClient() && mc.player == player && mc.getNetworkHandler() != null
                    && player.isOnGround() && !player.isTouchingWater() && !player.isClimbing()) {
                double x = player.getX(), y = player.getY(), z = player.getZ();
                boolean hc = player.horizontalCollision;
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, hc));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, hc));
            }
            return ActionResult.PASS;
        });
    }
}

final class AutoTotem extends Module {
    final NumSetting delay = num("Delay (Ticks)", 2, 0, 20, 1);
    final NumSetting health = num("Ab Leben <=", 20, 1, 20, 1);
    private int cd;

    AutoTotem() { super("AutoTotem", "Combat", "Totem automatisch in die Offhand nehmen"); }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null) return;
        ClientPlayerEntity p = mc.player;
        if (cd > 0) { cd--; return; }
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (p.getHealth() > health.value) return;
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (!s.isOf(Items.TOTEM_OF_UNDYING)) continue;
            int slot = i < 9 ? 36 + i : i;
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
            cd = (int) delay.value;
            return;
        }
    }
}

final class AutoClicker extends Module {
    final NumSetting cps = num("CPS", 12, 1, 20, 1);
    final BoolSetting cooldown = bool("Cooldown beachten", false);
    final BoolSetting random = bool("Zufall", true);
    private double acc;
    private final Random rnd = new Random();

    AutoClicker() { super("AutoClicker", "Combat", "Automatisches Klicken (Linksklick halten)"); }

    @Override public void onTick() {
        if (!inWorld() || mc.currentScreen != null || !mc.options.attackKey.isPressed()) { acc = 0; return; }
        ClientPlayerEntity p = mc.player;
        double c = cps.value * (random.value ? 0.8 + rnd.nextDouble() * 0.4 : 1.0);
        acc += c / 20.0;
        if (acc < 1.0) return;
        acc -= 1.0;
        if (cooldown.value && p.getAttackCooldownProgress(0.5f) < 0.9f) return;
        if (mc.crosshairTarget instanceof EntityHitResult h) {
            mc.interactionManager.attackEntity(p, h.getEntity());
            p.swingHand(Hand.MAIN_HAND);
        }
    }
}
