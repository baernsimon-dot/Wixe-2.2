package com.example.testclient.modules;

import com.example.testclient.settings.NumSetting;

/** Skaliert eingehenden Rueckstoss (wird vom NetHandlerMixin ausgewertet). */
public final class Velocity extends Module {
    public static Velocity INSTANCE;
    public final NumSetting horizontal = num("Horizontal %", 0, 0, 100, 5);
    public final NumSetting vertical = num("Vertikal %", 0, 0, 100, 5);

    public Velocity() { super("Velocity", "Combat", "Rueckstoss beeinflussen"); INSTANCE = this; }
}
