package com.example.lejos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.lejos.manufacturer.BleCompanyResolver;
import com.example.lejos.manufacturer.WifiOuiResolver;
import com.example.lejos.model.SignalObservation;
import com.example.lejos.scanner.AndroidBleSource;
import com.example.lejos.scanner.AndroidWifiSource;
import com.example.lejos.scanner.ObservationSource;
import com.example.lejos.ui.ObservationAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MainActivity extends AppCompatActivity implements ObservationSource.Listener {
    private final Map<String, SignalObservation> observations = new LinkedHashMap<>();
    private final List<ObservationSource> sources = new ArrayList<>();
    private ObservationAdapter adapter;
    private TextView statusView;
    private TextView countsView;
    private Button scanButton;
    private boolean scanning;

    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = true;
                for (Boolean value : result.values()) granted &= Boolean.TRUE.equals(value);
                if (granted) startSources();
                else Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show();
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusView = findViewById(R.id.status);
        countsView = findViewById(R.id.counts);
        scanButton = findViewById(R.id.scan_button);
        adapter = new ObservationAdapter(this);
        ListView list = findViewById(android.R.id.list);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(android.R.id.empty));

        sources.add(new AndroidWifiSource(this, new WifiOuiResolver()));
        sources.add(new AndroidBleSource(this, new BleCompanyResolver()));

        scanButton.setOnClickListener(view -> {
            if (scanning) stopSources(); else requestPermissionsAndStart();
        });
        findViewById(R.id.clear_button).setOnClickListener(view -> {
            observations.clear();
            render();
        });
    }

    private void requestPermissionsAndStart() {
        List<String> needed = new ArrayList<>();
        addIfMissing(needed, Manifest.permission.ACCESS_COARSE_LOCATION);
        addIfMissing(needed, Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addIfMissing(needed, Manifest.permission.BLUETOOTH_SCAN);
            addIfMissing(needed, Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (needed.isEmpty()) startSources();
        else permissions.launch(needed.toArray(new String[0]));
    }

    private void addIfMissing(List<String> needed, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            needed.add(permission);
        }
    }

    private void startSources() {
        scanning = true;
        scanButton.setText(R.string.stop);
        statusView.setText(R.string.status_listening);
        for (ObservationSource source : sources) source.start(this);
    }

    private void stopSources() {
        for (ObservationSource source : sources) source.stop();
        scanning = false;
        scanButton.setText(R.string.scan);
        statusView.setText(R.string.status_idle);
    }

    @Override public void onObservation(SignalObservation observation) {
        SignalObservation incoming = observation;
        runOnUiThread(() -> {
            SignalObservation updated = incoming;
            SignalObservation previous = observations.get(incoming.getStableKey());
            if (previous != null) updated = incoming.withFirstSeen(previous.getFirstSeenMillis());
            observations.put(updated.getStableKey(), updated);
            render();
        });
    }

    @Override public void onSourceStatus(String source, String status) {
        runOnUiThread(() -> statusView.setText(status));
    }

    private void render() {
        adapter.replace(observations.values());
        int wifi = 0;
        int ble = 0;
        for (SignalObservation item : observations.values()) {
            if (item.getKind() == SignalObservation.Kind.WIFI) wifi++;
            if (item.getKind() == SignalObservation.Kind.BLE) ble++;
        }
        countsView.setText(getString(R.string.observation_counts, wifi, ble));
    }

    @Override protected void onStop() {
        super.onStop();
        if (scanning) stopSources();
    }
}
