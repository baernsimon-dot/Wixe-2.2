package com.example.testclient.settings;

public class ModeSetting extends Setting {
    public final String[] options;
    public int index;

    public ModeSetting(String name, String... options) { super(name); this.options = options; }

    public String get() { return options[index]; }
    public boolean is(String s) { return options[index].equalsIgnoreCase(s); }
    public void next() { index = (index + 1) % options.length; }
    public void prev() { index = (index - 1 + options.length) % options.length; }
    public void set(String s) {
        for (int i = 0; i < options.length; i++) if (options[i].equalsIgnoreCase(s)) { index = i; return; }
    }
}
