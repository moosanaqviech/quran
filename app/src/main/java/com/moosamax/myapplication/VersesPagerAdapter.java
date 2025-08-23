package com.moosamax.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VersesPagerAdapter extends RecyclerView.Adapter<VersesPagerAdapter.VerseViewHolder> {

    private List<VerseData> verses;
    private OnVerseActionListener actionListener;

    public interface OnVerseActionListener {
        void onShareClick(VerseData verse);
        void onFavoriteClick(VerseData verse);
    }

    public VersesPagerAdapter(List<VerseData> verses) {
        this.verses = verses;
    }

    public void setActionListener(OnVerseActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public VerseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_verse_page, parent, false);
        return new VerseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VerseViewHolder holder, int position) {
        VerseData verse = verses.get(position);
        holder.bind(verse);
    }

    @Override
    public int getItemCount() {
        return verses.size();
    }

    class VerseViewHolder extends RecyclerView.ViewHolder {
        private TextView arabicTextView;
        private TextView englishTextView;
        private TextView referenceTextView;
        private TextView categoryTextView;
        private LinearLayout shareButton;
        private LinearLayout favoriteButton;
        private ImageView favoriteIcon;
        private TextView favoriteText;

        public VerseViewHolder(@NonNull View itemView) {
            super(itemView);
            arabicTextView = itemView.findViewById(R.id.verse_arabic);
            englishTextView = itemView.findViewById(R.id.verse_english);
            referenceTextView = itemView.findViewById(R.id.verse_reference);
            categoryTextView = itemView.findViewById(R.id.verse_category);
            shareButton = itemView.findViewById(R.id.share_verse_button);
            favoriteButton = itemView.findViewById(R.id.favorite_verse_button);
            favoriteIcon = itemView.findViewById(R.id.favorite_verse_icon);
            favoriteText = itemView.findViewById(R.id.favorite_verse_text);

            // Set click listeners
            if (shareButton != null) {
                shareButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && actionListener != null) {
                        actionListener.onShareClick(verses.get(position));
                    }
                });
            }

            if (favoriteButton != null) {
                favoriteButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && actionListener != null) {
                        actionListener.onFavoriteClick(verses.get(position));
                    }
                });
            }
        }

        public void bind(VerseData verse) {
            arabicTextView.setText(verse.getArabicText());
            englishTextView.setText(verse.getEnglishTranslation());
            referenceTextView.setText(verse.getReference());
            categoryTextView.setText(verse.getCategory());

            // Update favorite button state
            updateFavoriteButton(verse);
        }

        private void updateFavoriteButton(VerseData verse) {
            if (favoriteIcon != null && favoriteText != null) {
                FavoritesManager favoritesManager = FavoritesManager.getInstance(itemView.getContext());
                boolean isFavorite = favoritesManager.isFavorite(verse);

                if (isFavorite) {
                    favoriteIcon.setImageResource(android.R.drawable.btn_star_big_on);
                    favoriteText.setText("Favorited");
                } else {
                    favoriteIcon.setImageResource(android.R.drawable.btn_star_big_off);
                    favoriteText.setText("Favorite");
                }
            }
        }

        public void refreshFavoriteState(VerseData verse) {
            updateFavoriteButton(verse);
        }
    }
}