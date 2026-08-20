package com.example.lejos.model;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SignalObservationTest {
    @Test public void withFirstSeenPreservesLatestObservationData() {
        SignalObservation observation = new SignalObservation("wifi:key", SignalObservation.Kind.WIFI,
                "android-wifi", "Network", "00:00:00:00:00:00", "Vendor", -42,
                200L, 300L, Collections.singletonMap("channel", "6"));

        SignalObservation updated = observation.withFirstSeen(100L);

        assertEquals(100L, updated.getFirstSeenMillis());
        assertEquals(300L, updated.getLastSeenMillis());
        assertEquals(-42, updated.getRssi());
        assertEquals("6", updated.getAttributes().get("channel"));
    }
}
