package com.example.testclient.modules;

import com.example.testclient.TestClient;
import com.example.testclient.settings.*;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();
    public static final List<String> CATEGORIES = List.of("Movement", "Combat", "World", "Render");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModuleManager() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("testclient.json"); }

    public static void init() {
        // Movement
        MODULES.add(new Fly()); MODULES.add(new Speed()); MODULES.add(new Sprint()); MODULES.add(new NoFall());
        MODULES.add(new Jesus()); MODULES.add(new Step()); MODULES.add(new Spider()); MODULES.add(new HighJump());
        MODULES.add(new LongJump()); MODULES.add(new Dolphin());
        // Combat
        MODULES.add(new KillAura()); MODULES.add(new AimAssist()); MODULES.add(new TriggerBot()); MODULES.add(new Reach());
        MODULES.add(new Velocity()); MODULES.add(new Criticals()); MODULES.add(new AutoTotem()); MODULES.add(new AutoClicker());
        MODULES.add(new Hitbox()); MODULES.add(new AntiKnockback());
        // World & Utility
        MODULES.add(new XRay()); MODULES.add(new EntityEsp()); MODULES.add(new FullBright()); MODULES.add(new ChestEsp());
        MODULES.add(new Freecam()); MODULES.add(new Scaffold()); MODULES.add(new InventoryMove()); MODULES.add(new FastPlace());
        MODULES.add(new AutoFish()); MODULES.add(new Nuker()); MODULES.add(new SoundEsp()); MODULES.add(new BaseFinder());
        MODULES.add(new BlockScan());
        // Render / HUD
        MODULES.add(new Hud());
        boolean first = !Files.exists(file());
        load();
        if (first) for (Module m : MODULES) if (m instanceof Hud) m.restore(); // HUD beim ersten Start an
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (Module m : MODULES) {
            try {
                if (m.pending && mc.world != null && mc.player != null) { m.pending = false; m.onEnable(); }
                if (m.isEnabled() && !m.pending) m.onTick();
            } catch (Throwable t) {
                TestClient.LOGGER.error("Modul {} wurde wegen eines Fehlers deaktiviert", m.name, t);
                m.setEnabled(false);
            }
        }
    }

    public static void load() {
        try {
            if (!Files.exists(file())) return;
            JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();
            for (Module m : MODULES) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled") && o.get("enabled").getAsBoolean()) m.restore();
                for (Setting s : m.settings) {
                    if (!o.has(s.name)) continue;
                    JsonElement e = o.get(s.name);
                    if (s instanceof BoolSetting b) b.value = e.getAsBoolean();
                    else if (s instanceof NumSetting n) n.set(e.getAsDouble());
                    else if (s instanceof ModeSetting ms) ms.set(e.getAsString());
                }
            }
        } catch (Exception ex) {
            TestClient.LOGGER.warn("Konfiguration konnte nicht geladen werden", ex);
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : MODULES) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled());
                for (Setting s : m.settings) {
                    if (s instanceof BoolSetting b) o.addProperty(s.name, b.value);
                    else if (s instanceof NumSetting n) o.addProperty(s.name, n.value);
                    else if (s instanceof ModeSetting ms) o.addProperty(s.name, ms.get());
                }
                root.add(m.name, o);
            }
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception ex) {
            TestClient.LOGGER.warn("Konfiguration konnte nicht gespeichert werden", ex);
        }
    }
}
