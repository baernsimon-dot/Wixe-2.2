package com.example.testclient.util;

import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Stapelt kleine HUD-Panels links oben untereinander. */
public final class HudStack {
    private static int y = 6;
    private HudStack() {}

    public static void reset() { y = 6; }

    public static void panel(DrawContext c, String title, List<String> lines) {
        int w = Gfx.width(title);
        for (String l : lines) w = Math.max(w, Gfx.width(l));
        w += 14;
        int h = 22 + lines.size() * 11;
        Gfx.round(c, 6, y, w, h, 7, 0xB0101018);
        Gfx.text(c, title, 13, y + 6, 0xFF9B7BFF);
        int ly = y + 19;
        for (String l : lines) { Gfx.text(c, l, 13, ly, 0xFFE8E8F0); ly += 11; }
        y += h + 5;
    }
}
