package com.example.testclient.mixin;

import com.example.testclient.modules.Hitbox;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true, require = 0)
    private void tc$margin(CallbackInfoReturnable<Float> cir) {
        Hitbox h = Hitbox.INSTANCE;
        if (h != null) cir.setReturnValue(cir.getReturnValue() + h.extra((Entity) (Object) this));
    }
}
