package com.example.lejos.scanner;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.example.lejos.manufacturer.WifiOuiResolver;
import com.example.lejos.model.SignalObservation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AndroidWifiSource implements ObservationSource {
    private final Context context;
    private final WifiManager wifiManager;
    private final WifiOuiResolver ouiResolver;
    private Listener listener;
    private boolean receiverRegistered;

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            boolean fresh = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            publishResults(fresh);
        }
    };

    public AndroidWifiSource(Context context, WifiOuiResolver ouiResolver) {
        this.context = context.getApplicationContext();
        this.wifiManager = this.context.getSystemService(WifiManager.class);
        this.ouiResolver = ouiResolver;
    }

    @Override public String getSourceId() { return "android-wifi"; }

    @SuppressLint("MissingPermission")
    @Override public void start(Listener listener) {
        this.listener = listener;
        if (wifiManager == null) {
            listener.onSourceStatus(getSourceId(), "Wi-Fi unavailable");
            return;
        }
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(scanReceiver, filter);
            }
            receiverRegistered = true;
        }
        boolean accepted = wifiManager.startScan();
        listener.onSourceStatus(getSourceId(), accepted
                ? "Wi-Fi scan requested" : "Wi-Fi scan throttled; showing cached results");
        publishResults(false);
    }

    @Override public void stop() {
        if (receiverRegistered) {
            context.unregisterReceiver(scanReceiver);
            receiverRegistered = false;
        }
        listener = null;
    }

    @SuppressLint("MissingPermission")
    private void publishResults(boolean fresh) {
        Listener current = listener;
        if (current == null || wifiManager == null) return;
        List<ScanResult> results = wifiManager.getScanResults();
        long now = System.currentTimeMillis();
        for (ScanResult result : results) {
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("frequency", result.frequency + " MHz");
            attributes.put("channel", Integer.toString(channelForFrequency(result.frequency)));
            attributes.put("security", result.capabilities == null ? "" : result.capabilities);
            String ssid = result.SSID == null || result.SSID.isEmpty() ? "Hidden network" : result.SSID;
            current.onObservation(new SignalObservation(
                    "wifi:" + result.BSSID,
                    SignalObservation.Kind.WIFI,
                    getSourceId(), ssid, result.BSSID, ouiResolver.resolve(result.BSSID),
                    result.level, now, now, attributes));
        }
        current.onSourceStatus(getSourceId(), results.size() + " Wi-Fi APs" + (fresh ? " (fresh)" : ""));
    }

    static int channelForFrequency(int frequency) {
        if (frequency == 2484) return 14;
        if (frequency >= 2412 && frequency <= 2472) return (frequency - 2407) / 5;
        if (frequency >= 5000 && frequency <= 5895) return (frequency - 5000) / 5;
        if (frequency >= 5955 && frequency <= 7115) return (frequency - 5950) / 5;
        return 0;
    }
}
