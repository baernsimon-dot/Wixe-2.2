package com.example.testclient.modules;

import com.example.testclient.settings.*;
import com.example.testclient.util.Move;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class Fly extends Module {
    final ModeSetting mode = mode("Modus", "Creative", "Velocity");
    final NumSetting speed = num("Speed", 1.0, 0.1, 10, 0.1);

    Fly() { super("Fly", "Movement", "Fliegen"); }

    @Override public void onTick() {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        if (mode.is("Creative")) {
            PlayerAbilities a = p.getAbilities();
            a.allowFlying = true;
            a.flying = true;
            a.setFlySpeed((float) (0.05 * speed.value));
        } else {
            double s = speed.value * 0.5;
            double[] d = Move.dir(mc);
            double vy = 0;
            if (mc.options.jumpKey.isPressed()) vy += s;
            if (mc.options.sneakKey.isPressed()) vy -= s;
            p.setVelocity(d[0] * s, vy, d[1] * s);
        }
    }

    @Override public void onDisable() {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().allowFlying = false;
            p.getAbilities().flying = false;
        }
        p.getAbilities().setFlySpeed(0.05f);
    }
}

final class Speed extends Module {
    final NumSetting speed = num("Speed", 1.5, 1.0, 8, 0.1);
    final BoolSetting onGround = bool("Nur am Boden", false);

    Speed() { super("Speed", "Movement", "Schneller laufen"); }

    @Override public void onTick() {
        if (!inWorld() || !Move.moving(mc)) return;
        ClientPlayerEntity p = mc.player;
        if (onGround.value && !p.isOnGround()) return;
        double s = 0.2873 * speed.value;
        double[] d = Move.dir(mc);
        Vec3d v = p.getVelocity();
        p.setVelocity(d[0] * s, v.y, d[1] * s);
    }
}

final class Sprint extends Module {
    final BoolSetting omni = bool("Alle Richtungen", false);

    Sprint() { super("Sprint", "Movement", "Sprintet automatisch beim Vorwaertslaufen"); }

    @Override public void onTick() {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        boolean move = omni.value ? Move.moving(mc) : mc.options.forwardKey.isPressed();
        if (move && !p.isSneaking() && p.getHungerManager().getFoodLevel() > 6) p.setSprinting(true);
    }
}

final class NoFall extends Module {
    NoFall() { super("NoFall", "Movement", "Kein Fallschaden (Boden-Status wird gemeldet)"); }

    @Override public void onTick() {
        if (!inWorld() || mc.getNetworkHandler() == null) return;
        ClientPlayerEntity p = mc.player;
        if (p.fallDistance > 2.0 && !p.isOnGround()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
        }
    }
}

final class Jesus extends Module {
    Jesus() { super("Jesus", "Movement", "Ueber Wasser laufen"); }

    @Override public void onTick() {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        if (p.isSneaking()) return;
        Vec3d v = p.getVelocity();
        if (p.isTouchingWater()) {
            p.setVelocity(v.x, Math.max(v.y, 0.1), v.z);
        } else {
            BlockPos under = BlockPos.ofFloored(p.getX(), p.getY() - 0.1, p.getZ());
            if (!mc.world.getFluidState(under).isEmpty() && v.y < 0) {
                p.setVelocity(v.x, 0, v.z);
                p.setOnGround(true);
            }
        }
    }
}

final class Step extends AttrModule {
    final NumSetting height = num("Hoehe", 1.0, 1.0, 5.0, 0.5);

    Step() { super("Step", "Movement", "Hoehere Bloecke automatisch erklimmen"); }

    @Override public void onTick() {
        if (inWorld()) apply(EntityAttributes.STEP_HEIGHT, "step", height.value - 0.6);
    }
    @Override public void onDisable() { clear(EntityAttributes.STEP_HEIGHT, "step"); }
}

final class Spider extends Module {
    final NumSetting speed = num("Speed", 0.2, 0.05, 0.6, 0.05);

    Spider() { super("Spider", "Movement", "An Waenden hochklettern"); }

    @Override public void onTick() {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        if (p.horizontalCollision && !p.isSneaking()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, speed.value, v.z);
        }
    }
}

final class HighJump extends AttrModule {
    final NumSetting mult = num("Faktor", 2.0, 1.0, 6.0, 0.25);

    HighJump() { super("HighJump", "Movement", "Hoeher springen"); }

    @Override public void onTick() {
        if (inWorld()) apply(EntityAttributes.JUMP_STRENGTH, "highjump", (mult.value - 1.0) * 0.42);
    }
    @Override public void onDisable() { clear(EntityAttributes.JUMP_STRENGTH, "highjump"); }
}

final class LongJump extends Module {
    final NumSetting boost = num("Boost", 1.5, 0.5, 5, 0.1);
    private boolean wasGround;

    LongJump() { super("LongJump", "Movement", "Weiter springen"); }

    @Override public void onTick() {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        if (wasGround && !p.isOnGround() && p.getVelocity().y > 0 && Move.moving(mc)) {
            double s = 0.3 * boost.value;
            double[] d = Move.dir(mc);
            p.setVelocity(d[0] * s, p.getVelocity().y, d[1] * s);
        }
        wasGround = p.isOnGround();
    }
}

final class Dolphin extends Module {
    final NumSetting speed = num("Speed", 1.5, 1.0, 4, 0.1);

    Dolphin() { super("Dolphin", "Movement", "Schneller schwimmen"); }

    @Override public void onTick() {
        if (!inWorld() || !Move.moving(mc)) return;
        ClientPlayerEntity p = mc.player;
        if (!p.isTouchingWater()) return;
        double s = 0.13 * speed.value;
        double[] d = Move.dir(mc);
        p.setVelocity(d[0] * s, p.getVelocity().y, d[1] * s);
    }
}
