package com.example.lejos.grouping;

import com.example.lejos.model.SignalObservation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Conservatively collapses observations that look like radios belonging to one physical AP.
 * Raw observations are never mutated or discarded; callers can re-group them at any time.
 */
public final class WifiObservationGrouper {
    private static final long MAX_TIME_DELTA_MILLIS = 90_000L;
    private static final int MAX_RSSI_DELTA_DBM = 12;

    public List<SignalObservation> group(Collection<SignalObservation> observations) {
        List<SignalObservation> wifi = new ArrayList<>();
        List<SignalObservation> result = new ArrayList<>();
        for (SignalObservation observation : observations) {
            if (observation.getKind() == SignalObservation.Kind.WIFI) wifi.add(observation);
            else result.add(observation);
        }

        List<List<SignalObservation>> clusters = new ArrayList<>();
        for (SignalObservation candidate : wifi) {
            List<SignalObservation> compatibleCluster = null;
            for (List<SignalObservation> cluster : clusters) {
                boolean compatibleWithEveryMember = true;
                for (SignalObservation member : cluster) {
                    if (!shouldGroup(candidate, member)) {
                        compatibleWithEveryMember = false;
                        break;
                    }
                }
                if (compatibleWithEveryMember) {
                    compatibleCluster = cluster;
                    break;
                }
            }
            if (compatibleCluster == null) {
                compatibleCluster = new ArrayList<>();
                clusters.add(compatibleCluster);
            }
            compatibleCluster.add(candidate);
        }
        for (List<SignalObservation> cluster : clusters) {
            result.add(cluster.size() == 1 ? cluster.get(0) : merge(cluster));
        }
        return result;
    }

    boolean shouldGroup(SignalObservation left, SignalObservation right) {
        String leftSsid = left.getDisplayName();
        String rightSsid = right.getDisplayName();
        int ssidRelationship = ssidRelationship(leftSsid, rightSsid);
        if (ssidRelationship == 0) return false;
        if (!security(left).equals(security(right))) return false;

        long timeDelta = Math.abs(left.getLastSeenMillis() - right.getLastSeenMillis());
        if (timeDelta > MAX_TIME_DELTA_MILLIS) return false;
        int rssiDelta = Math.abs(left.getRssi() - right.getRssi());
        if (rssiDelta > MAX_RSSI_DELTA_DBM) return false;

        int commonBytes = commonMacPrefixBytes(left.getAddress(), right.getAddress());
        boolean complementaryBands = band(left) != 0 && band(right) != 0 && band(left) != band(right);

        // A five-byte prefix is typical of adjacent BSSIDs allocated to one device.
        if (commonBytes >= 5) return true;
        // Related names such as "Home" and "Home-5G" need a strong address relationship.
        if (ssidRelationship == 1) return commonBytes >= 4 && complementaryBands;
        // Exact SSIDs are common in dense environments, so same-vendor alone is insufficient.
        return commonBytes >= 4 || (commonBytes >= 3 && complementaryBands && rssiDelta <= 8);
    }

    private SignalObservation merge(List<SignalObservation> members) {
        members.sort(Comparator.comparing(SignalObservation::getStableKey));
        SignalObservation strongest = Collections.max(members,
                Comparator.comparingInt(SignalObservation::getRssi));
        long firstSeen = Long.MAX_VALUE;
        long lastSeen = 0;
        TreeSet<String> addresses = new TreeSet<>();
        TreeSet<String> channels = new TreeSet<>();
        TreeSet<String> bands = new TreeSet<>();
        StringBuilder key = new StringBuilder("wifi-group:");
        for (SignalObservation member : members) {
            firstSeen = Math.min(firstSeen, member.getFirstSeenMillis());
            lastSeen = Math.max(lastSeen, member.getLastSeenMillis());
            addresses.add(member.getAddress());
            addPresent(channels, member.getAttributes().get("channel"));
            int band = band(member);
            if (band != 0) bands.add(band == 2 ? "2.4 GHz" : band + " GHz");
            key.append(member.getStableKey()).append('|');
        }

        Map<String, String> attributes = new LinkedHashMap<>(strongest.getAttributes());
        attributes.put("radios", Integer.toString(members.size()));
        attributes.put("BSSIDs", String.join(", ", addresses));
        attributes.put("bands", String.join(" + ", bands));
        attributes.put("channels", String.join(", ", channels));
        return new SignalObservation(key.toString(), SignalObservation.Kind.WIFI,
                "wifi-heuristic-group", preferredName(members),
                members.size() + " related BSSIDs", strongest.getManufacturer(),
                strongest.getRssi(), firstSeen, lastSeen, attributes);
    }

    private static String preferredName(List<SignalObservation> members) {
        String preferred = members.get(0).getDisplayName();
        for (SignalObservation member : members) {
            if (member.getDisplayName().length() < preferred.length()) preferred = member.getDisplayName();
        }
        return preferred;
    }

    private static int ssidRelationship(String left, String right) {
        if (isHidden(left) || isHidden(right)) return 0;
        if (left.equalsIgnoreCase(right)) return 2;
        return normalizeSsid(left).equals(normalizeSsid(right)) ? 1 : 0;
    }

    static String normalizeSsid(String ssid) {
        String normalized = ssid.trim().toLowerCase(Locale.US);
        return normalized.replaceFirst("(?i)([-_ .]?(2[._]?4g|2g|5g|5ghz|6g|6ghz))$", "")
                .replaceAll("[-_ .]", "");
    }

    private static boolean isHidden(String ssid) {
        return ssid == null || ssid.isEmpty() || "Hidden network".equals(ssid);
    }

    private static String security(SignalObservation observation) {
        String value = observation.getAttributes().get("security");
        return value == null ? "" : value.replaceAll("\\s", "").toUpperCase(Locale.US);
    }

    private static int band(SignalObservation observation) {
        String value = observation.getAttributes().get("frequency");
        if (value == null) return 0;
        try {
            int frequency = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            if (frequency >= 2400 && frequency < 2500) return 2;
            if (frequency >= 4900 && frequency < 5900) return 5;
            if (frequency >= 5900 && frequency < 7200) return 6;
        } catch (NumberFormatException ignored) {
            // Unknown/malformed frequency cannot contribute to grouping.
        }
        return 0;
    }

    static int commonMacPrefixBytes(String left, String right) {
        byte[] leftBytes = macBytes(left);
        byte[] rightBytes = macBytes(right);
        if (leftBytes == null || rightBytes == null) return 0;
        int common = 0;
        while (common < leftBytes.length && leftBytes[common] == rightBytes[common]) common++;
        return common;
    }

    private static byte[] macBytes(String address) {
        if (address == null) return null;
        String normalized = address.replace(":", "").replace("-", "");
        if (normalized.length() != 12) return null;
        byte[] bytes = new byte[6];
        try {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(normalized.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void addPresent(TreeSet<String> values, String value) {
        if (value != null && !value.isEmpty() && !"0".equals(value)) values.add(value);
    }
}
