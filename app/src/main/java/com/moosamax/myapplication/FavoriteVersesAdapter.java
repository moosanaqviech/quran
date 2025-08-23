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

public class FavoriteVersesAdapter extends RecyclerView.Adapter<FavoriteVersesAdapter.FavoriteVerseViewHolder> {

    private List<VerseData> verses;
    private OnVerseActionListener listener;

    public interface OnVerseActionListener {
        void onVerseClick(VerseData verse);
        void onShareClick(VerseData verse);
        void onFavoriteClick(VerseData verse);
    }

    public FavoriteVersesAdapter(List<VerseData> verses, OnVerseActionListener listener) {
        this.verses = verses;
        this.listener = listener;
    }

    public void updateVerses(List<VerseData> newVerses) {
        this.verses = newVerses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteVerseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_verse, parent, false);
        return new FavoriteVerseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteVerseViewHolder holder, int position) {
        VerseData verse = verses.get(position);
        holder.bind(verse);
    }

    @Override
    public int getItemCount() {
        return verses.size();
    }

    class FavoriteVerseViewHolder extends RecyclerView.ViewHolder {
        private TextView arabicTextView;
        private TextView englishTextView;
        private TextView referenceTextView;
        private TextView categoryTextView;
        private LinearLayout shareButton;
        private LinearLayout favoriteButton;
        private ImageView favoriteIcon;

        public FavoriteVerseViewHolder(@NonNull View itemView) {
            super(itemView);
            arabicTextView = itemView.findViewById(R.id.verse_arabic);
            englishTextView = itemView.findViewById(R.id.verse_english);
            referenceTextView = itemView.findViewById(R.id.verse_reference);
            categoryTextView = itemView.findViewById(R.id.verse_category);
            shareButton = itemView.findViewById(R.id.share_button);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onVerseClick(verses.get(position));
                }
            });

            shareButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onShareClick(verses.get(position));
                }
            });

            favoriteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onFavoriteClick(verses.get(position));
                }
            });
        }

        public void bind(VerseData verse) {
            arabicTextView.setText(verse.getArabicText());
            englishTextView.setText(verse.getEnglishTranslation());
            referenceTextView.setText(verse.getReference());
            categoryTextView.setText(verse.getCategory());

            // Set favorite icon (always filled since these are favorites)
            favoriteIcon.setImageResource(android.R.drawable.btn_star_big_on);
        }
    }
}