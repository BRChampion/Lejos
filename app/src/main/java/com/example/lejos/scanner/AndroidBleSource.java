package com.example.lejos.scanner;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.os.Build;
import android.util.SparseArray;

import com.example.lejos.manufacturer.BleCompanyResolver;
import com.example.lejos.model.SignalObservation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AndroidBleSource implements ObservationSource {
    private final BluetoothAdapter adapter;
    private final BleCompanyResolver companyResolver;
    private BluetoothLeScanner scanner;
    private Listener listener;

    private final ScanCallback callback = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            publish(result);
        }

        @Override public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) publish(result);
        }

        @Override public void onScanFailed(int errorCode) {
            if (listener != null) listener.onSourceStatus(getSourceId(), "BLE scan error " + errorCode);
        }
    };

    public AndroidBleSource(Context context, BleCompanyResolver companyResolver) {
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        this.adapter = manager == null ? null : manager.getAdapter();
        this.companyResolver = companyResolver;
    }

    @Override public String getSourceId() { return "android-ble"; }

    @SuppressLint("MissingPermission")
    @Override public void start(Listener listener) {
        this.listener = listener;
        if (adapter == null || !adapter.isEnabled()) {
            listener.onSourceStatus(getSourceId(), "Bluetooth unavailable or disabled");
            return;
        }
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            listener.onSourceStatus(getSourceId(), "BLE scanner unavailable");
            return;
        }
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
        scanner.startScan(null, settings, callback);
        listener.onSourceStatus(getSourceId(), "BLE listening");
    }

    @SuppressLint("MissingPermission")
    @Override public void stop() {
        if (scanner != null) scanner.stopScan(callback);
        scanner = null;
        listener = null;
    }

    @SuppressLint("MissingPermission")
    private void publish(ScanResult result) {
        Listener current = listener;
        if (current == null) return;
        ScanRecord record = result.getScanRecord();
        SparseArray<byte[]> manufacturerData = record == null ? null : record.getManufacturerSpecificData();
        Integer companyId = manufacturerData == null || manufacturerData.size() == 0
                ? null : manufacturerData.keyAt(0);
        String name = record == null ? null : record.getDeviceName();
        if (name == null || name.isEmpty()) name = "Unnamed BLE device";

        Map<String, String> attributes = new LinkedHashMap<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            attributes.put("connectable", Boolean.toString(result.isConnectable()));
        }
        if (companyId != null) attributes.put("companyId", String.format("0x%04X", companyId));
        if (record != null && record.getServiceUuids() != null) {
            List<String> uuids = new ArrayList<>();
            for (ParcelUuid uuid : record.getServiceUuids()) uuids.add(uuid.toString());
            attributes.put("services", String.join(", ", uuids));
        }
        long now = System.currentTimeMillis();
        String address = result.getDevice().getAddress();
        current.onObservation(new SignalObservation(
                "ble:" + address,
                SignalObservation.Kind.BLE,
                getSourceId(), name, address, companyResolver.resolve(companyId),
                result.getRssi(), now, now, attributes));
    }
}
