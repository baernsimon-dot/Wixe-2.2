package com.example.testclient.modules;

import com.example.testclient.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name, category, description;
    public final List<Setting> settings = new ArrayList<>();
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean enabled;
    public boolean pending;   // beim Laden aktiv, onEnable folgt sobald eine Welt da ist
    public float anim;        // GUI-Animation

    protected Module(String name, String category, String description) {
        this.name = name; this.category = category; this.description = description;
    }

    protected BoolSetting bool(String n, boolean v) { BoolSetting s = new BoolSetting(n, v); settings.add(s); return s; }
    protected NumSetting num(String n, double v, double min, double max, double step) { NumSetting s = new NumSetting(n, v, min, max, step); settings.add(s); return s; }
    protected ModeSetting mode(String n, String... o) { ModeSetting s = new ModeSetting(n, o); settings.add(s); return s; }

    protected boolean inWorld() { return mc.player != null && mc.world != null; }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }
    public void restore() { enabled = true; pending = true; }
    public void setEnabled(boolean b) {
        if (enabled == b) return;
        enabled = b; pending = false;
        if (b) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onHud(DrawContext ctx) {}
}
