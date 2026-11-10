package com.example.testclient.modules;

import com.example.testclient.settings.NumSetting;

public final class AntiKnockback extends Module {
    public static AntiKnockback INSTANCE;
    public final NumSetting strength = num("Staerke %", 60, 0, 100, 5);

    public AntiKnockback() { super("AntiKnockback", "Combat", "Rueckstoss reduzieren"); INSTANCE = this; }
}
