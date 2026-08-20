package com.example.lejos.manufacturer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class BleCompanyResolver {
    private final Map<Integer, String> companies;

    public BleCompanyResolver() {
        Map<Integer, String> values = new HashMap<>();
        values.put(0x004C, "Apple");
        values.put(0x0006, "Microsoft");
        values.put(0x000F, "Broadcom");
        values.put(0x0075, "Samsung Electronics");
        values.put(0x00E0, "Google");
        companies = Collections.unmodifiableMap(values);
    }

    public String resolve(Integer companyId) {
        if (companyId == null) return "Unknown manufacturer";
        return companies.getOrDefault(companyId,
                String.format("Bluetooth company 0x%04X", companyId));
    }
}
