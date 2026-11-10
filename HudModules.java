package com.example.testclient.modules;

import com.example.testclient.settings.BoolSetting;
import com.example.testclient.util.Gfx;
import com.example.testclient.util.HudStack;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Ein HUD mit Watermark, Koordinaten, FPS, Blickrichtung und Modulliste. */
final class Hud extends Module {
    final BoolSetting watermark = bool("Watermark", true);
    final BoolSetting coords = bool("Koordinaten", true);
    final BoolSetting fps = bool("FPS", true);
    final BoolSetting facing = bool("Blickrichtung", true);
    final BoolSetting list = bool("Modulliste", true);

    Hud() { super("HUD", "Render", "Watermark, Koordinaten, FPS und Modulliste"); }

    @Override public void onHud(DrawContext ctx) {
        if (mc.player == null) return;

        List<String> l = new ArrayList<>();
        if (coords.value) l.add(String.format(Locale.ROOT, "XYZ %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        if (facing.value) l.add("Richtung " + mc.player.getHorizontalFacing().asString());
        if (fps.value) l.add("FPS " + mc.getCurrentFps());
        if (watermark.value || !l.isEmpty()) HudStack.panel(ctx, watermark.value ? "TestClient 2.1" : "Info", l);

        if (list.value) {
            List<Module> on = new ArrayList<>();
            for (Module m : ModuleManager.MODULES) if (m.isEnabled() && !m.pending && m != this) on.add(m);
            on.sort(Comparator.comparingInt((Module m) -> -Gfx.width(m.name)));
            int sw = mc.getWindow().getScaledWidth(), y = 6;
            for (Module m : on) {
                int w = Gfx.width(m.name) + 12;
                Gfx.round(ctx, sw - w - 6, y, w, 14, 6, 0xB0101018);
                Gfx.text(ctx, m.name, sw - w, y + 3, 0xFF9B7BFF);
                y += 16;
            }
        }
    }
}
