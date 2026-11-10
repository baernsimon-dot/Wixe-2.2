package com.example.testclient.modules;

import com.example.testclient.settings.BoolSetting;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

import java.util.HashSet;
import java.util.Set;

public final class XRay extends Module {
    public static XRay INSTANCE;
    final BoolSetting diamond = bool("Diamant", true), gold = bool("Gold", true), iron = bool("Eisen", true),
            coal = bool("Kohle", false), redstone = bool("Redstone", true), lapis = bool("Lapis", true),
            emerald = bool("Smaragd", true), copper = bool("Kupfer", false), quartz = bool("Quarz", false),
            debris = bool("Ancient Debris", true), spawner = bool("Spawner", true), chests = bool("Kisten", true),
            bright = bool("Hell", true);
    private volatile Set<Block> visible = new HashSet<>();
    private int signature = -1;

    public XRay() { super("X-Ray", "World", "Erze durch Bloecke sehen (nicht mit Sodium)"); INSTANCE = this; }

    /** Vom BlockMixin aufgerufen. */
    public boolean shows(Block b) { return visible.contains(b); }

    private int sig() {
        int s = 0, i = 0;
        for (var st : settings) { if (st instanceof BoolSetting b && b.value) s |= 1 << i; i++; }
        return s;
    }

    private void rebuild() {
        Set<Block> set = new HashSet<>();
        for (Block b : Registries.BLOCK) {
            String p = Registries.BLOCK.getId(b).getPath();
            if ((diamond.value && p.endsWith("diamond_ore")) || (gold.value && p.endsWith("gold_ore"))
                || (iron.value && p.endsWith("iron_ore")) || (coal.value && p.endsWith("coal_ore"))
                || (redstone.value && p.endsWith("redstone_ore")) || (lapis.value && p.endsWith("lapis_ore"))
                || (emerald.value && p.endsWith("emerald_ore")) || (copper.value && p.endsWith("copper_ore"))
                || (quartz.value && p.equals("nether_quartz_ore")) || (debris.value && p.equals("ancient_debris"))
                || (spawner.value && p.equals("spawner"))
                || (chests.value && (p.equals("chest") || p.equals("trapped_chest") || p.equals("ender_chest") || p.equals("barrel")))) {
                set.add(b);
            }
        }
        visible = set;
        signature = sig();
    }

    private void reload() { if (mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload(); }

    @Override public void onEnable() { rebuild(); reload(); }
    @Override public void onDisable() { reload(); }

    @Override public void onTick() {
        if (!inWorld()) return;
        if (sig() != signature) { rebuild(); reload(); }
        if (bright.value) FullBright.giveNightVision(mc);
    }
}
