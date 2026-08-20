package com.example.lejos.scanner;

import com.example.lejos.model.SignalObservation;

public interface ObservationSource {
    interface Listener {
        void onObservation(SignalObservation observation);
        void onSourceStatus(String source, String status);
    }

    String getSourceId();
    void start(Listener listener);
    void stop();
}
