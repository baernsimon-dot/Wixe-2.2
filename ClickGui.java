package com.example.testclient.gui;

import com.example.testclient.modules.Module;
import com.example.testclient.modules.ModuleManager;
import com.example.testclient.settings.*;
import com.example.testclient.util.Gfx;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClickGui extends Screen {
    private static final int W = 560, H = 350;
    private static final int BG = 0xF2101018, PANEL = 0xFF15151E, CARD = 0xFF1D1D2A, CARD_HOVER = 0xFF252536,
            SUB = 0xFF181824, ACCENT = 0xFF8B6BFF, OFF = 0xFF3A3A4E, GRAY = 0xFF8C8CA6, WHITE = 0xFFFFFFFF, DIM = 0xFFC9C9DA;

    private record Hit(int x, int y, int w, int h, int kind, Module m, Setting s) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private String category = ModuleManager.CATEGORIES.get(0);
    private Module expanded;
    private double scroll;
    private int contentH, viewX, viewW, viewTop, viewBottom;
    private NumSetting dragging;
    private TextFieldWidget search;
    private String query = "";
    private int dragX, dragW;

    public ClickGui() { super(Text.literal("TestClient")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        search = new TextFieldWidget(this.textRenderer, x0 + 122, y0 + 14, 166, 10, Text.literal("Suche"));
        search.setDrawsBackground(false);
        search.setMaxLength(32);
        search.setPlaceholder(Text.literal("Module suchen..."));
        search.setChangedListener(s -> { query = s; scroll = 0; });
        search.setText(query);
        addDrawableChild(search);
    }

    private boolean matches(Module m) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return m.category.equals(category);
        return m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)
            || m.category.toLowerCase(Locale.ROOT).contains(q);
    }

    @Override public void removed() { ModuleManager.save(); }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        hits.clear();
        ctx.fill(0, 0, width, height, 0x78000000);
        Gfx.round(ctx, x0 - 1, y0 - 1, W + 2, H + 2, 14, 0x40FFFFFF);
        Gfx.round(ctx, x0, y0, W, H, 13, BG);

        // Header
        Gfx.text(ctx, "TestClient", x0 + 18, y0 + 14, ACCENT);
        Gfx.text(ctx, "v2.1", x0 + 18 + Gfx.width("TestClient") + 6, y0 + 14, GRAY);
        String hint = "LMB an/aus   RMB Optionen";
        Gfx.text(ctx, hint, x0 + W - 18 - Gfx.width(hint), y0 + 14, GRAY);

        // Suchleiste
        Gfx.round(ctx, x0 + 110, y0 + 8, 190, 22, 10, search.isFocused() ? 0xFF2A2A40 : CARD);
        search.render(ctx, mx, my, delta);

        // Sidebar
        int sy = y0 + 42;
        for (String cat : ModuleManager.CATEGORIES) {
            boolean sel = query.isEmpty() && cat.equals(category);
            boolean hov = mx >= x0 + 12 && mx < x0 + 128 && my >= sy && my < sy + 28;
            Gfx.round(ctx, x0 + 12, sy, 116, 28, 9, sel ? ACCENT : hov ? CARD_HOVER : CARD);
            Gfx.text(ctx, cat, x0 + 24, sy + 10, sel ? WHITE : DIM);
            long on = ModuleManager.MODULES.stream().filter(m -> m.category.equals(cat) && m.isEnabled()).count();
            if (on > 0) {
                String s = String.valueOf(on);
                int bw = Gfx.width(s) + 8;
                Gfx.round(ctx, x0 + 128 - bw - 8, sy + 7, bw, 14, 7, sel ? 0x55FFFFFF : 0xFF2C2C40);
                Gfx.text(ctx, s, x0 + 128 - bw - 4, sy + 10, WHITE);
            }
            hits.add(new Hit(x0 + 12, sy, 116, 28, 0, null, null));
            sy += 34;
        }

        // Content
        viewX = x0 + 142; viewW = W - 142 - 14; viewTop = y0 + 42; viewBottom = y0 + H - 14;
        Gfx.round(ctx, viewX - 6, viewTop - 6, viewW + 12, viewBottom - viewTop + 12, 11, PANEL);
        ctx.enableScissor(viewX, viewTop, viewX + viewW, viewBottom);
        int y = viewTop - (int) scroll;
        for (Module m : ModuleManager.MODULES) {
            if (!matches(m)) continue;
            y = drawModule(ctx, m, y, mx, my);
        }
        if (y == viewTop - (int) scroll) Gfx.text(ctx, "Keine Treffer", viewX + 12, viewTop + 10, GRAY);
        contentH = y + (int) scroll - viewTop;
        ctx.disableScissor();

        // Scrollbar
        int viewH = viewBottom - viewTop;
        double max = Math.max(0, contentH - viewH);
        scroll = Math.max(0, Math.min(max, scroll));
        if (max > 0) {
            int bh = Math.max(24, (int) (viewH * (viewH / (double) contentH)));
            int by = viewTop + (int) ((viewH - bh) * (scroll / max));
            Gfx.round(ctx, viewX + viewW + 1, by, 3, bh, 2, 0x88FFFFFF);
        }
    }

    private int drawModule(DrawContext ctx, Module m, int y, int mx, int my) {
        int h = 34;
        boolean inView = my >= viewTop && my < viewBottom && mx >= viewX && mx < viewX + viewW;
        boolean hov = inView && mx >= viewX && mx < viewX + viewW && my >= y && my < y + h;
        m.anim += ((m.isEnabled() ? 1f : 0f) - m.anim) * 0.3f;

        Gfx.round(ctx, viewX, y, viewW, h, 9, hov ? CARD_HOVER : CARD);
        if (m.anim > 0.02f) Gfx.round(ctx, viewX, y + 8, 3, h - 16, 1, Gfx.lerp(0x00000000 | (ACCENT & 0xFFFFFF), ACCENT, m.anim));
        Gfx.text(ctx, m.name, viewX + 12, y + 7, Gfx.lerp(DIM, WHITE, m.anim));
        Gfx.text(ctx, m.description, viewX + 12, y + 19, GRAY);

        int sx = viewX + viewW - 14 - 32, sy = y + 10;
        drawSwitch(ctx, sx, sy, m.anim);
        if (!m.settings.isEmpty()) Gfx.text(ctx, expanded == m ? "-" : "+", sx - 14, y + 13, GRAY);
        hits.add(new Hit(viewX, y, viewW, h, 1, m, null));
        y += h;

        if (expanded == m && !m.settings.isEmpty()) {
            int total = 6;
            for (Setting s : m.settings) total += rowHeight(s);
            Gfx.round(ctx, viewX + 6, y + 3, viewW - 12, total, 9, SUB);
            int ry = y + 6;
            for (Setting s : m.settings) {
                int rh = rowHeight(s);
                drawSetting(ctx, s, viewX + 6, ry, viewW - 12, rh, mx, my, inView);
                hits.add(new Hit(viewX + 6, ry, viewW - 12, rh, 2, m, s));
                ry += rh;
            }
            y += total + 3;
        }
        return y + 6;
    }

    private int rowHeight(Setting s) { return s instanceof NumSetting ? 30 : 24; }

    private void drawSwitch(DrawContext ctx, int x, int y, float t) {
        Gfx.round(ctx, x, y, 32, 14, 7, Gfx.lerp(OFF, ACCENT, t));
        Gfx.round(ctx, x + 2 + (int) (18 * t), y + 2, 10, 10, 5, WHITE);
    }

    private void drawSetting(DrawContext ctx, Setting s, int x, int y, int w, int h, int mx, int my, boolean inView) {
        if (s instanceof BoolSetting b) {
            Gfx.text(ctx, b.name, x + 12, y + 8, DIM);
            drawSwitch(ctx, x + w - 12 - 32, y + 5, b.value ? 1f : 0f);
        } else if (s instanceof ModeSetting ms) {
            Gfx.text(ctx, ms.name, x + 12, y + 8, DIM);
            String v = ms.get();
            int pw = Gfx.width(v) + 18;
            Gfx.round(ctx, x + w - 12 - pw, y + 3, pw, 18, 8, CARD);
            Gfx.text(ctx, v, x + w - 12 - pw + 9, y + 8, ACCENT);
        } else if (s instanceof NumSetting n) {
            Gfx.text(ctx, n.name, x + 12, y + 5, DIM);
            String v = n.format();
            Gfx.text(ctx, v, x + w - 12 - Gfx.width(v), y + 5, ACCENT);
            int tx = x + 12, tw = w - 24, ty = y + 19;
            double t = (n.value - n.min) / (n.max - n.min);
            Gfx.round(ctx, tx, ty, tw, 5, 2, OFF);
            Gfx.round(ctx, tx, ty, Math.max(5, (int) (tw * t)), 5, 2, ACCENT);
            Gfx.round(ctx, tx + (int) (tw * t) - 4, ty - 2, 9, 9, 4, WHITE);
        }
    }

    // ----------------------------------------------------------------- input

    private void setFromMouse(NumSetting n, double mouseX) {
        double t = (mouseX - dragX) / (double) dragW;
        t = Math.max(0, Math.min(1, t));
        n.set(n.min + t * (n.max - n.min));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int b = click.button();
        if (search != null && search.mouseClicked(click, doubled)) {
            search.setFocused(true);
            setFocused(search);
            return true;
        }
        if (search != null) { search.setFocused(false); setFocused(null); }
        for (Hit h : hits) {
            if (!h.in(mx, my)) continue;
            if (h.kind == 0) {
                category = ModuleManager.CATEGORIES.get(indexOfCategory(h));
                scroll = 0; expanded = null;
                if (search != null) search.setText("");
                return true;
            }
            if (my < viewTop || my >= viewBottom) continue;
            if (h.kind == 1) {
                if (b == 0) h.m.toggle();
                else if (b == 1 && !h.m.settings.isEmpty()) expanded = expanded == h.m ? null : h.m;
                return true;
            }
            if (h.kind == 2) {
                Setting s = h.s;
                if (s instanceof BoolSetting bs) bs.value = !bs.value;
                else if (s instanceof ModeSetting ms) { if (b == 0) ms.next(); else ms.prev(); }
                else if (s instanceof NumSetting ns) {
                    dragging = ns; dragX = h.x + 12; dragW = h.w - 24;
                    setFromMouse(ns, mx);
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private int indexOfCategory(Hit h) {
        int idx = 0;
        for (Hit o : hits) {
            if (o.kind != 0) continue;
            if (o == h) return idx;
            idx++;
        }
        return 0;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) { setFromMouse(dragging, click.x()); return true; }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 22;
        return true;
    }
}
