package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemNewsCardBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<NewsItem> masterNewsList;
    private final List<NewsItem> filteredNewsList;

    public NewsAdapter(List<NewsItem> newsItems) {
        this.masterNewsList = new ArrayList<>(newsItems);
        this.filteredNewsList = new ArrayList<>(newsItems);
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNewsCardBinding binding = ItemNewsCardBinding.inflate(inflater, parent, false);
        return new NewsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem currentItem = filteredNewsList.get(position);
        holder.bind(currentItem);
    }

    @Override
    public int getItemCount() {
        return filteredNewsList.size();
    }

    // UPDATED: This is a more powerful filter method
    public void filter(String category, String query) {
        filteredNewsList.clear();
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault());

        for (NewsItem item : masterNewsList) {
            // Check if the item belongs to the selected category
            if (item.getCategory().equalsIgnoreCase(category)) {
                // If there's a search query, check if the title contains it
                if (!lowerCaseQuery.isEmpty()) {
                    if (item.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                        filteredNewsList.add(item);
                    }
                } else {
                    // If the query is empty, just add all items from the category
                    filteredNewsList.add(item);
                }
            }
        }
        // Refresh the display
        notifyDataSetChanged();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsCardBinding binding;

        public NewsViewHolder(ItemNewsCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(NewsItem newsItem) {
            binding.cardTitle.setText(newsItem.getTitle());
            binding.cardDescription.setText(newsItem.getDescription());
            binding.cardTimestamp.setText(newsItem.getTimestamp());
            binding.cardImage.setImageResource(newsItem.getImageResource());
        }
    }
}
