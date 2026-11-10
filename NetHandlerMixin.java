package com.example.testclient.mixin;

import com.example.testclient.modules.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class NetHandlerMixin {
    @Unique private Vec3d tc$prev;

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), require = 0)
    private void tc$velHead(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.isOnThread() && packet.getEntityId() == mc.player.getId()) tc$prev = mc.player.getVelocity();
        else tc$prev = null;
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"), require = 0)
    private void tc$velTail(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (tc$prev == null || mc.player == null) return;
        double h = 1, v = 1;
        Velocity vel = Velocity.INSTANCE;
        if (vel != null && vel.isEnabled()) { h = vel.horizontal.value / 100.0; v = vel.vertical.value / 100.0; }
        AntiKnockback ak = AntiKnockback.INSTANCE;
        if (ak != null && ak.isEnabled()) { double k = 1 - ak.strength.value / 100.0; h *= k; v *= k; }
        if (h != 1 || v != 1) {
            Vec3d now = mc.player.getVelocity();
            mc.player.setVelocity(tc$prev.x + (now.x - tc$prev.x) * h, tc$prev.y + (now.y - tc$prev.y) * v, tc$prev.z + (now.z - tc$prev.z) * h);
        }
        tc$prev = null;
    }

    @Inject(method = "onPlaySound", at = @At("HEAD"), require = 0)
    private void tc$sound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread()) return;
        String id = packet.getSound().getIdAsString();
        if (SoundEsp.INSTANCE != null) SoundEsp.INSTANCE.add(packet.getX(), packet.getY(), packet.getZ(), id);
        if (AutoFish.INSTANCE != null && id.contains("fishing_bobber.splash")) AutoFish.INSTANCE.onSplash(packet.getX(), packet.getY(), packet.getZ());
    }
}
