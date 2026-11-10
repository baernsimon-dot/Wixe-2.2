package com.example.testclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Gfx {
    private Gfx() {}

    /** Abgerundetes Rechteck (ARGB). */
    public static void round(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    public static void outline(DrawContext c, int l, int t, int r, int b, int color) {
        c.fill(l, t, r, t + 1, color);
        c.fill(l, b - 1, r, b, color);
        c.fill(l, t, l + 1, b, color);
        c.fill(r - 1, t, r, b, color);
    }

    public static void text(DrawContext c, String s, int x, int y, int color) {
        c.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, s, x, y, color);
    }

    public static int width(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
