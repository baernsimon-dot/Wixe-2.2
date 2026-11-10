package com.example.testclient.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class Boxes {
    private Boxes() {}

    /** Zeichnet die projizierte 2D-Huelle einer Box. */
    public static void box2d(DrawContext c, Box b, int color, String label) {
        double l = 1e9, t = 1e9, r = -1e9, bt = -1e9;
        double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
        for (double x : xs) for (double y : ys) for (double z : zs) {
            double[] s = Proj.project(new Vec3d(x, y, z));
            if (s == null) return;
            l = Math.min(l, s[0]); r = Math.max(r, s[0]);
            t = Math.min(t, s[1]); bt = Math.max(bt, s[1]);
        }
        Gfx.outline(c, (int) l, (int) t, (int) r + 1, (int) bt + 1, color);
        if (label != null) Gfx.text(c, label, (int) l, (int) t - 10, color);
    }
}
