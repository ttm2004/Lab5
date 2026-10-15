package com.trantrongmanh.lab5.news;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.trantrongmanh.lab5.R;
import com.trantrongmanh.lab5.model.NewsItem;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NewsItem item);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position, NewsItem item);
    }

    private final Context context;
    private List<NewsItem> items = new ArrayList<>();
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private int lastAnimatedPosition = -1;

    public NewsAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener l)     { this.clickListener     = l; }
    public void setOnItemLongClickListener(OnItemLongClickListener l) { this.longClickListener = l; }

    public void setItems(List<NewsItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    public NewsItem getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        NewsItem item = items.get(position);

        h.tvTitle.setText(item.getTitle());
        h.tvSnippet.setText(item.getSnippet());
        h.tvPubDate.setText(item.getPubDate() != null ? item.getPubDate() : "");
        h.tvSource.setText(item.getSource() != null ? item.getSource() : "");

        // Thumbnail image via Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            h.ivThumbnail.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(h.ivThumbnail);
        } else {
            h.ivThumbnail.setVisibility(View.GONE);
        }

        // Click
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(item);
        });

        // Long click (for context menu)
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(v, position, item);
            }
            return false;
        });

        // Slide-in animation per item
        if (position > lastAnimatedPosition) {
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
            h.itemView.startAnimation(anim);
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView  tvTitle, tvSnippet, tvPubDate, tvSource;

        ViewHolder(View v) {
            super(v);
            ivThumbnail = v.findViewById(R.id.ivNewsThumbnail);
            tvTitle     = v.findViewById(R.id.tvNewsTitle);
            tvSnippet   = v.findViewById(R.id.tvNewsSnippet);
            tvPubDate   = v.findViewById(R.id.tvNewsPubDate);
            tvSource    = v.findViewById(R.id.tvNewsSource);
        }
    }
}
