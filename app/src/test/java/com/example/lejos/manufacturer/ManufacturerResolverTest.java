package com.example.lejos.manufacturer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ManufacturerResolverTest {
    @Test public void wifiResolverNormalizesMacSeparatorsAndCase() {
        WifiOuiResolver resolver = new WifiOuiResolver();
        assertEquals("Espressif", resolver.resolve("24:a1:60:12:34:56"));
        assertEquals("Google", resolver.resolve("3c-5a-b4-00-00-00"));
    }

    @Test public void wifiResolverReturnsUnknownForUnlistedOui() {
        assertEquals("Unknown manufacturer", new WifiOuiResolver().resolve("12:34:56:78:9A:BC"));
    }

    @Test public void bleResolverUsesCompanyIdentifier() {
        assertEquals("Apple", new BleCompanyResolver().resolve(0x004C));
        assertEquals("Bluetooth company 0x1234", new BleCompanyResolver().resolve(0x1234));
    }
}
