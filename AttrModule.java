package com.example.testclient.modules;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Basis fuer Module, die Spieler-Attribute (Step, Reach, Jump ...) per temporaerem Modifier aendern. */
public abstract class AttrModule extends Module {
    private final Map<String, Double> cache = new HashMap<>();
    private Object lastPlayer;

    protected AttrModule(String name, String category, String description) { super(name, category, description); }

    protected void apply(RegistryEntry<EntityAttribute> attr, String id, double value) {
        if (mc.player == null) return;
        if (mc.player != lastPlayer) { cache.clear(); lastPlayer = mc.player; }
        Double old = cache.get(id);
        if (old != null && old == value) return;
        EntityAttributeInstance inst = mc.player.getAttributeInstance(attr);
        if (inst == null) return;
        Identifier key = Identifier.of("testclient", id);
        inst.removeModifier(key);
        inst.addTemporaryModifier(new EntityAttributeModifier(key, value, EntityAttributeModifier.Operation.ADD_VALUE));
        cache.put(id, value);
    }

    protected void clear(RegistryEntry<EntityAttribute> attr, String id) {
        cache.remove(id);
        if (mc.player == null) return;
        EntityAttributeInstance inst = mc.player.getAttributeInstance(attr);
        if (inst != null) inst.removeModifier(Identifier.of("testclient", id));
    }
}
