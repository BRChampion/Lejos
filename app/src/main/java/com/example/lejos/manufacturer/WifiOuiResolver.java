package com.example.lejos.manufacturer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class WifiOuiResolver {
    private final Map<String, String> vendors;

    public WifiOuiResolver() {
        Map<String, String> values = new HashMap<>();
        values.put("001A11", "Google");
        values.put("3C5AB4", "Google");
        values.put("F4F5D8", "Google");
        values.put("0017F2", "Apple");
        values.put("3C22FB", "Apple");
        values.put("F0D1A9", "Apple");
        values.put("001632", "Samsung Electronics");
        values.put("8C7712", "Samsung Electronics");
        values.put("B827EB", "Raspberry Pi Foundation");
        values.put("DCA632", "Raspberry Pi Trading");
        values.put("24A160", "Espressif");
        values.put("246F28", "Espressif");
        values.put("7CDFA1", "Espressif");
        vendors = Collections.unmodifiableMap(values);
    }

    public String resolve(String macAddress) {
        if (macAddress == null) return "Unknown manufacturer";
        String normalized = macAddress.replace(":", "").replace("-", "").toUpperCase(Locale.US);
        if (normalized.length() < 6) return "Unknown manufacturer";
        return vendors.getOrDefault(normalized.substring(0, 6), "Unknown manufacturer");
    }
}
