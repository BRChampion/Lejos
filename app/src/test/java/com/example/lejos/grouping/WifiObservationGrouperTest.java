package com.example.lejos.grouping;

import com.example.lejos.model.SignalObservation;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WifiObservationGrouperTest {
    private final WifiObservationGrouper grouper = new WifiObservationGrouper();

    @Test public void groupsAdjacentDualBandBssids() {
        SignalObservation twoG = wifi("Home", "AA:BB:CC:DD:EE:10", -48, 1000, 2412, "1", "[WPA2][ESS]");
        SignalObservation fiveG = wifi("Home", "AA:BB:CC:DD:EE:11", -52, 1200, 5180, "36", "[WPA2][ESS]");

        List<SignalObservation> result = grouper.group(Arrays.asList(twoG, fiveG));

        assertEquals(1, result.size());
        assertEquals("2", result.get(0).getAttributes().get("radios"));
        assertEquals("2.4 GHz + 5 GHz", result.get(0).getAttributes().get("bands"));
        assertTrue(result.get(0).getAttributes().get("BSSIDs").contains(twoG.getAddress()));
    }

    @Test public void groupsRelatedBandSuffixesWithStrongMacRelationship() {
        SignalObservation twoG = wifi("Cabin-2.4G", "AA:BB:CC:DD:10:01", -60, 1000, 2437, "6", "WPA2");
        SignalObservation fiveG = wifi("Cabin_5G", "AA:BB:CC:DD:20:01", -63, 1100, 5220, "44", "WPA2");

        assertEquals(1, grouper.group(Arrays.asList(twoG, fiveG)).size());
    }

    @Test public void doesNotGroupSameSsidFromUnrelatedAddresses() {
        SignalObservation first = wifi("Cafe", "AA:BB:CC:10:00:01", -55, 1000, 2412, "1", "WPA2");
        SignalObservation second = wifi("Cafe", "11:22:33:44:55:66", -56, 1100, 5180, "36", "WPA2");

        assertEquals(2, grouper.group(Arrays.asList(first, second)).size());
    }

    @Test public void doesNotGroupDifferentSecurityOrDistantRssi() {
        SignalObservation first = wifi("Home", "AA:BB:CC:DD:EE:01", -40, 1000, 2412, "1", "WPA2");
        SignalObservation differentSecurity = wifi("Home", "AA:BB:CC:DD:EE:02", -42, 1100, 5180, "36", "WPA3");
        SignalObservation distant = wifi("Home", "AA:BB:CC:DD:EE:03", -80, 1100, 5180, "36", "WPA2");

        assertEquals(3, grouper.group(Arrays.asList(first, differentSecurity, distant)).size());
    }

    @Test public void doesNotChainIncompatibleRadiosThroughMiddleMember() {
        SignalObservation first = wifi("Home", "AA:BB:CC:DD:EE:01", -40, 1000, 2412, "1", "WPA2");
        SignalObservation middle = wifi("Home", "AA:BB:CC:DD:EE:02", -50, 1100, 5180, "36", "WPA2");
        SignalObservation last = wifi("Home", "AA:BB:CC:DD:EE:03", -60, 1200, 2417, "2", "WPA2");

        assertEquals(2, grouper.group(Arrays.asList(first, middle, last)).size());
    }

    @Test public void normalizesCommonBandSuffixesOnlyAtEnd() {
        assertEquals("myhome", WifiObservationGrouper.normalizeSsid("My Home-5GHz"));
        assertEquals("myhome", WifiObservationGrouper.normalizeSsid("My_Home_2.4G"));
    }

    private static SignalObservation wifi(String ssid, String bssid, int rssi, long seen,
            int frequency, String channel, String security) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("frequency", frequency + " MHz");
        attributes.put("channel", channel);
        attributes.put("security", security);
        return new SignalObservation("wifi:" + bssid, SignalObservation.Kind.WIFI,
                "android-wifi", ssid, bssid, "Test vendor", rssi, seen, seen, attributes);
    }
}
