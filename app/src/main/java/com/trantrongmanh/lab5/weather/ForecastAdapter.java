package com.trantrongmanh.lab5.weather;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.trantrongmanh.lab5.R;
import com.trantrongmanh.lab5.model.ForecastResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private final Context context;
    private List<ForecastResponse.ForecastItem> items = new ArrayList<>();

    // Show one entry per day (noon slot)
    private static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("EEE, dd/MM", new Locale("vi", "VN"));

    public ForecastAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<ForecastResponse.ForecastItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_forecast, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ForecastResponse.ForecastItem item = items.get(position);

        // Day label
        Date date = new Date(item.getTimestamp() * 1000L);
        h.tvDay.setText(DAY_FORMAT.format(date));

        // Description
        h.tvDesc.setText(item.getDescription());

        // Icon
        Glide.with(context)
                .load(item.getIconUrl())
                .placeholder(android.R.mipmap.sym_def_app_icon)
                .into(h.ivIcon);

        // Temperatures
        if (item.getMain() != null) {
            h.tvMax.setText("↑ " + item.getMain().getTempMaxCelsius());
            h.tvMin.setText("↓ " + item.getMain().getTempMinCelsius());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView  tvDay, tvDesc, tvMax, tvMin;
        ImageView ivIcon;

        ViewHolder(View v) {
            super(v);
            tvDay  = v.findViewById(R.id.tvForecastDay);
            tvDesc = v.findViewById(R.id.tvForecastDesc);
            tvMax  = v.findViewById(R.id.tvForecastMax);
            tvMin  = v.findViewById(R.id.tvForecastMin);
            ivIcon = v.findViewById(R.id.ivForecastIcon);
        }
    }
}
