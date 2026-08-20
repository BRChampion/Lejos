package com.example.lejos.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.lejos.R;
import com.example.lejos.model.SignalObservation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ObservationAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final List<SignalObservation> items = new ArrayList<>();

    public ObservationAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void replace(Collection<SignalObservation> observations) {
        items.clear();
        items.addAll(observations);
        items.sort(Comparator.comparingInt(SignalObservation::getRssi).reversed());
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public SignalObservation getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return getItem(position).getStableKey().hashCode(); }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView == null
                ? inflater.inflate(R.layout.item_observation, parent, false) : convertView;
        SignalObservation item = getItem(position);
        ((TextView) view.findViewById(R.id.title)).setText(item.getDisplayName());
        ((TextView) view.findViewById(R.id.subtitle)).setText(context.getString(
                R.string.observation_summary, item.getKind(), item.getManufacturer(), item.getRssi()));
        StringBuilder details = new StringBuilder(item.getAddress());
        for (Map.Entry<String, String> entry : item.getAttributes().entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                details.append("\n").append(entry.getKey()).append(": ").append(entry.getValue());
            }
        }
        ((TextView) view.findViewById(R.id.details)).setText(details.toString());
        return view;
    }
}
