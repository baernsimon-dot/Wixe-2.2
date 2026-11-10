package com.example.testclient.mixin;

import com.example.testclient.modules.XRay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0)
    private static void tc$xray(BlockState state, BlockState neighbor, Direction side, CallbackInfoReturnable<Boolean> cir) {
        XRay x = XRay.INSTANCE;
        if (x != null && x.isEnabled()) cir.setReturnValue(x.shows(state.getBlock()));
    }
}
