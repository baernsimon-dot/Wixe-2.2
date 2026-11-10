package com.example.testclient.modules;

import com.example.testclient.settings.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.Hand;

public final class AutoFish extends Module {
    public static AutoFish INSTANCE;
    final NumSetting delay = num("Delay (Ticks)", 6, 0, 40, 1);
    final BoolSetting recast = bool("Neu auswerfen", true);
    private int reel = -1, cast = -1;

    public AutoFish() { super("AutoFish", "World", "Automatisch angeln"); INSTANCE = this; }

    /** Vom NetHandlerMixin bei "fishing_bobber.splash" aufgerufen. */
    public void onSplash(double x, double y, double z) {
        if (!isEnabled() || mc.player == null || mc.player.fishHook == null) return;
        if (mc.player.fishHook.squaredDistanceTo(x, y, z) > 4.0) return;
        reel = (int) delay.value;
    }

    private void use(ClientPlayerEntity p) {
        mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
        p.swingHand(Hand.MAIN_HAND);
    }

    @Override public void onTick() {
        if (!inWorld() || mc.interactionManager == null) return;
        ClientPlayerEntity p = mc.player;
        if (!(p.getMainHandStack().getItem() instanceof FishingRodItem)) return;
        if (reel > 0) reel--;
        else if (reel == 0) { use(p); reel = -1; if (recast.value) cast = 25; }
        if (cast > 0 && --cast == 0 && p.fishHook == null) use(p);
    }
}
