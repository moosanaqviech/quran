package com.moosamax.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class RecentVersesManager {
    private static final String PREFS_NAME = "RecentVerses";
    private static final String KEY_RECENT_VERSES = "recent_verse_references";
    private static final String KEY_RECENT_TIMESTAMPS = "recent_timestamps";
    private static final int MAX_RECENT_VERSES = 15;

    private Context context;
    private SharedPreferences prefs;

    public RecentVersesManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Add a verse to recent history
     */
    public void addRecentVerse(VerseData verse) {
        if (verse == null || verse.getReference() == null) {
            return;
        }

        List<String> recentRefs = getRecentVerseReferences();
        List<Long> timestamps = getRecentTimestamps();

        String verseRef = verse.getReference();
        long currentTime = System.currentTimeMillis();

        // Remove if already exists (to move to top)
        int existingIndex = recentRefs.indexOf(verseRef);
        if (existingIndex != -1) {
            recentRefs.remove(existingIndex);
            timestamps.remove(existingIndex);
        }

        // Add to beginning
        recentRefs.add(0, verseRef);
        timestamps.add(0, currentTime);

        // Keep only MAX_RECENT_VERSES
        while (recentRefs.size() > MAX_RECENT_VERSES) {
            recentRefs.remove(recentRefs.size() - 1);
            timestamps.remove(timestamps.size() - 1);
        }

        // Save to preferences
        saveRecentVerses(recentRefs, timestamps);

        Log.d("RecentVersesManager", "Added recent verse: " + verseRef +
                " (total: " + recentRefs.size() + ")");
    }

    /**
     * Get list of recent verses (up to 15)
     */
    public List<VerseData> getRecentVerses() {
        List<String> recentRefs = getRecentVerseReferences();
        List<VerseData> recentVerses = new ArrayList<>();
        List<VerseData> allVerses = VerseRepository.getAllVerses();

        for (String ref : recentRefs) {
            for (VerseData verse : allVerses) {
                if (verse.getReference().equals(ref)) {
                    recentVerses.add(verse);
                    break;
                }
            }
        }

        return recentVerses;
    }

    /**
     * Get recent verse count
     */
    public int getRecentVerseCount() {
        return getRecentVerseReferences().size();
    }

    /**
     * Clear all recent verses
     */
    public void clearRecentVerses() {
        prefs.edit()
                .remove(KEY_RECENT_VERSES)
                .remove(KEY_RECENT_TIMESTAMPS)
                .apply();
        Log.d("RecentVersesManager", "Cleared all recent verses");
    }

    /**
     * Get formatted time for recent verse (e.g., "2 hours ago")
     */
    public String getTimeAgo(int index) {
        List<Long> timestamps = getRecentTimestamps();
        if (index < timestamps.size()) {
            long timestamp = timestamps.get(index);
            long timeDiff = System.currentTimeMillis() - timestamp;
            return formatTimeAgo(timeDiff);
        }
        return "Unknown time";
    }

    private String formatTimeAgo(long timeDiff) {
        long minutes = timeDiff / (1000 * 60);
        long hours = timeDiff / (1000 * 60 * 60);
        long days = timeDiff / (1000 * 60 * 60 * 24);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return "Over a week ago";
        }
    }

    private List<String> getRecentVerseReferences() {
        String recentRefs = prefs.getString(KEY_RECENT_VERSES, "");
        List<String> refs = new ArrayList<>();
        if (!recentRefs.isEmpty()) {
            String[] refArray = recentRefs.split(",");
            for (String ref : refArray) {
                if (!ref.trim().isEmpty()) {
                    refs.add(ref.trim());
                }
            }
        }
        return refs;
    }

    private List<Long> getRecentTimestamps() {
        String timestamps = prefs.getString(KEY_RECENT_TIMESTAMPS, "");
        List<Long> times = new ArrayList<>();
        if (!timestamps.isEmpty()) {
            String[] timeArray = timestamps.split(",");
            for (String time : timeArray) {
                try {
                    times.add(Long.parseLong(time.trim()));
                } catch (NumberFormatException e) {
                    Log.w("RecentVersesManager", "Invalid timestamp: " + time);
                }
            }
        }
        return times;
    }

    private void saveRecentVerses(List<String> references, List<Long> timestamps) {
        String refsString = String.join(",", references);

        List<String> timestampStrings = new ArrayList<>();
        for (Long timestamp : timestamps) {
            timestampStrings.add(String.valueOf(timestamp));
        }
        String timestampsString = String.join(",", timestampStrings);

        prefs.edit()
                .putString(KEY_RECENT_VERSES, refsString)
                .putString(KEY_RECENT_TIMESTAMPS, timestampsString)
                .apply();
    }
}