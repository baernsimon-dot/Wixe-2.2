package com.example.testclient.mixin;

import com.example.testclient.modules.Freecam;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class PlayerMixin {
    /** Freecam: dem Server keine Bewegung melden. */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true, require = 0)
    private void tc$noMove(CallbackInfo ci) {
        Freecam f = Freecam.INSTANCE;
        if (f != null && f.isEnabled()) ci.cancel();
    }
}
