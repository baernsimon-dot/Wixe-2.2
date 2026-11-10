package com.example.testclient.settings;

import java.util.Locale;

public class NumSetting extends Setting {
    public double value;
    public final double min, max, step;

    public NumSetting(String name, double value, double min, double max, double step) {
        super(name);
        this.min = min; this.max = max; this.step = step;
        set(value);
    }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) v = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, v));
    }

    public String format() {
        return step >= 1 ? String.valueOf((int) Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
    }
}
