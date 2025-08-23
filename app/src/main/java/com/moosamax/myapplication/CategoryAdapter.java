package com.moosamax.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<String> categories;
    private OnCategoryClickListener listener;
    private Map<String, String> categoryEmojis;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
        initializeCategoryEmojis();
    }

    private void initializeCategoryEmojis() {
        categoryEmojis = new HashMap<>();
        categoryEmojis.put("Trust in Allah", "🤲");
        categoryEmojis.put("Hope & Patience", "🌅");
        categoryEmojis.put("Mercy & Forgiveness", "💚");
        categoryEmojis.put("Dua & Supplication", "🤲");
        categoryEmojis.put("Patience & Perseverance", "💪");
        categoryEmojis.put("Guidance", "🌟");
        categoryEmojis.put("Prayer", "🕌");
        categoryEmojis.put("Gratitude", "🙏");
        categoryEmojis.put("Protection", "🛡️");
        categoryEmojis.put("Wisdom", "📚");
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        int verseCount = VerseRepository.getVerseCountByCategory(category);

        holder.bind(category, verseCount);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView emojiTextView;
        private TextView categoryNameTextView;
        private TextView verseCountTextView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            emojiTextView = itemView.findViewById(R.id.category_emoji);
            categoryNameTextView = itemView.findViewById(R.id.category_name);
            verseCountTextView = itemView.findViewById(R.id.verse_count);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClick(categories.get(position));
                }
            });
        }

        public void bind(String category, int verseCount) {
            // Set emoji
            String emoji = categoryEmojis.getOrDefault(category, "📖");
            emojiTextView.setText(emoji);

            // Set category name
            categoryNameTextView.setText(category);

            // Set verse count
            String countText = verseCount == 1 ? "1 verse" : verseCount + " verses";
            verseCountTextView.setText(countText);
        }
    }
}