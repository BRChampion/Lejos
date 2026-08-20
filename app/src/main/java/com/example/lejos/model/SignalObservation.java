package com.example.lejos.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SignalObservation {
    public enum Kind { WIFI, BLE, EXTERNAL }

    private final String stableKey;
    private final Kind kind;
    private final String source;
    private final String displayName;
    private final String address;
    private final String manufacturer;
    private final int rssi;
    private final long firstSeenMillis;
    private final long lastSeenMillis;
    private final Map<String, String> attributes;

    public SignalObservation(String stableKey, Kind kind, String source, String displayName,
            String address, String manufacturer, int rssi, long firstSeenMillis,
            long lastSeenMillis, Map<String, String> attributes) {
        this.stableKey = stableKey;
        this.kind = kind;
        this.source = source;
        this.displayName = displayName;
        this.address = address;
        this.manufacturer = manufacturer;
        this.rssi = rssi;
        this.firstSeenMillis = firstSeenMillis;
        this.lastSeenMillis = lastSeenMillis;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public String getStableKey() { return stableKey; }
    public Kind getKind() { return kind; }
    public String getSource() { return source; }
    public String getDisplayName() { return displayName; }
    public String getAddress() { return address; }
    public String getManufacturer() { return manufacturer; }
    public int getRssi() { return rssi; }
    public long getFirstSeenMillis() { return firstSeenMillis; }
    public long getLastSeenMillis() { return lastSeenMillis; }
    public Map<String, String> getAttributes() { return attributes; }

    public SignalObservation withFirstSeen(long firstSeen) {
        return new SignalObservation(stableKey, kind, source, displayName, address, manufacturer,
                rssi, firstSeen, lastSeenMillis, attributes);
    }
}
