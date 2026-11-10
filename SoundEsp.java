package com.example.testclient.modules;

import com.example.testclient.settings.*;
import com.example.testclient.util.Gfx;
import com.example.testclient.util.Proj;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Zeigt, woher Geraeusche kommen (Schritte, Bloecke, Entities). */
public final class SoundEsp extends Module {
    public static SoundEsp INSTANCE;
    final BoolSetting steps = bool("Schritte", true), blocks = bool("Bloecke", true),
            entities = bool("Entities", true), other = bool("Sonstiges", false);
    final NumSetting duration = num("Dauer (s)", 4, 1, 15, 1), dist = num("Reichweite", 48, 8, 128, 4);
    private record Ping(Vec3d pos, String name, long until) {}
    private final List<Ping> pings = new CopyOnWriteArrayList<>();

    public SoundEsp() { super("Sound ESP", "World", "Zeigt woher Geraeusche kommen"); INSTANCE = this; }

    /** Vom NetHandlerMixin aufgerufen. */
    public void add(double x, double y, double z, String id) {
        if (!isEnabled() || mc.player == null) return;
        String path = id.replace("minecraft:", "");
        boolean ok;
        if (path.contains("step")) ok = steps.value;
        else if (path.startsWith("block.") || path.contains(".place") || path.contains(".break")) ok = blocks.value;
        else if (path.startsWith("entity.") || path.startsWith("item.")) ok = entities.value;
        else ok = other.value;
        if (!ok) return;
        Vec3d p = new Vec3d(x, y, z);
        double d = mc.player.getPos().distanceTo(p);
        if (d < 1.5 || d > dist.value) return;
        pings.add(new Ping(p, path, System.currentTimeMillis() + (long) (duration.value * 1000)));
        if (pings.size() > 80) pings.remove(0);
    }

    @Override public void onEnable() { pings.clear(); }

    @Override public void onTick() {
        long now = System.currentTimeMillis();
        pings.removeIf(p -> p.until < now);
    }

    @Override public void onHud(DrawContext ctx) {
        if (!inWorld()) return;
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();
        for (Ping p : pings) {
            float life = Math.max(0f, Math.min(1f, (p.until - now) / (float) (duration.value * 1000)));
            int a = 80 + (int) (175 * life);
            int col = (a << 24) | 0x5CE1FF;
            String d = (int) mc.player.getPos().distanceTo(p.pos) + "m";
            double[] s = Proj.project(p.pos);
            String shortName = p.name.length() > 24 ? p.name.substring(p.name.length() - 24) : p.name;
            if (s != null && s[0] > 0 && s[0] < w && s[1] > 0 && s[1] < h) {
                Gfx.round(ctx, (int) s[0] - 3, (int) s[1] - 3, 7, 7, 3, col);
                Gfx.text(ctx, shortName + " " + d, (int) s[0] + 7, (int) s[1] - 4, col);
            } else {
                double ang = Proj.angle(p.pos);
                int rx = (int) (w / 2 + Math.sin(ang) * 70), ry = (int) (h / 2 - Math.cos(ang) * 70);
                Gfx.round(ctx, rx - 4, ry - 4, 9, 9, 4, col);
                Gfx.text(ctx, d, rx + 7, ry - 4, col);
            }
        }
    }
}
