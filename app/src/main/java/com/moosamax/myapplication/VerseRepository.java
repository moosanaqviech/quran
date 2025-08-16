package com.moosamax.myapplication;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VerseRepository {
    private static VerseRepository instance;
    private static List<VerseData> verses = new ArrayList<>();
    private static boolean isInitialized = false;
    private static final Random random = new Random();
    private static Context context;
    private VerseRepository(Context context) {
        this.context = context.getApplicationContext();

    }

    public static synchronized VerseRepository getInstance(Context context) {
        if (instance == null) {
            instance = new VerseRepository(context);
        }
        return instance;
    }

    /**
     * Initialize verses from assets file
     * This method loads verses from a JSON or CSV file in the assets folder
     */
    public static void initialize() {
        if (!isInitialized) {
            loadVersesFromAssets();
            isInitialized = true;
        }
    }

    /**
     * Load verses from assets/quran_verses.csv file
     * CSV format: "Arabic Text","English Translation","Reference","Category"
     */
    private static void loadVersesFromAssets() {
        verses.clear();

        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("quran_verses.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header line if present
                if (lineNumber == 1 && line.startsWith("Arabic")) {
                    continue;
                }

                try {
                    VerseData verse = parseCSVLine(line);
                    if (verse != null) {
                        verses.add(verse);
                    }
                } catch (Exception e) {
                    // Log error but continue processing other verses
                    System.err.println("Error parsing line " + lineNumber + ": " + line);
                    e.printStackTrace();
                }
            }

            reader.close();
            inputStream.close();

            System.out.println("Successfully loaded " + verses.size() + " verses from assets");

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to hardcoded sample verses if file loading fails
            loadFallbackVerses();
        }
    }

    /**
     * Parse a CSV line into a VerseData object
     * Handles quoted fields and comma separation properly
     */
    private static VerseData parseCSVLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Handle escaped quotes
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        fields.add(currentField.toString().trim());

        if (fields.size() >= 4) {
            String arabic = fields.get(0);
            String english = fields.get(1);
            String reference = fields.get(2);
            String category = fields.get(3);
            String origin = fields.get(4);

            return new VerseData(arabic, english, reference, category, origin);
        }

        return null;
    }

    /**
     * Fallback method with sample verses if asset loading fails
     */
    private static void loadFallbackVerses() {
        verses.clear();

        // Add some sample verses as fallback
        verses.add(new VerseData(
                "وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا",
                "And whoever fears Allah - He will make for him a way out.",
                "At-Talaq 65:2",
                "Trust in Allah",
                "origin"
        ));

        verses.add(new VerseData(
                "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا",
                "For indeed, with hardship [will be] ease.",
                "Ash-Sharh 94:5",
                "Hope & Patience",
                "origin"
        ));

        verses.add(new VerseData(
                "وَاللَّهُ غَفُورٌ رَّحِيمٌ",
                "And Allah is Forgiving and Merciful.",
                "Al-Baqarah 2:173",
                "Mercy & Forgiveness",
                "origin"
        ));

        verses.add(new VerseData(
                "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                "Our Lord, give us good in this world and good in the hereafter and protect us from the punishment of the Fire.",
                "Al-Baqarah 2:201",
                "Dua & Supplication",
                "origin"
        ));

        verses.add(new VerseData(
                "وَبَشِّرِ الصَّابِرِينَ",
                "And give good tidings to the patient.",
                "Al-Baqarah 2:155",
                "Patience & Perseverance",
                "origin"
        ));

        verses.add(new VerseData(
                "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",
                "Indeed, Allah is with the patient.",
                "Al-Baqarah 2:153",
                "Patience & Perseverance",
                "origin"
        ));

        System.out.println("Loaded fallback verses: " + verses.size());
    }

    /**
     * Get all verses
     */
    public static List<VerseData> getAllVerses() {
        return new ArrayList<>(verses);
    }

    /**
     * Get a random verse
     */
    public static VerseData getRandomVerse() {
        if (verses.isEmpty()) {
            throw new IllegalStateException("Verses not initialized. Call initialize(context) first.");
        }

        int randomIndex = random.nextInt(verses.size());
        return verses.get(randomIndex);
    }

    /**
     * Get verses by category
     */
    public static List<VerseData> getVersesByCategory(String category) {
        List<VerseData> categoryVerses = new ArrayList<>();

        for (VerseData verse : verses) {
            if (verse.getCategory().equals(category)) {
                categoryVerses.add(verse);
            }
        }

        return categoryVerses;
    }

    /**
     * Get all unique categories
     */
    public static List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();

        for (VerseData verse : verses) {
            String category = verse.getCategory();
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }

        return categories;
    }

    /**
     * Get verse count by category
     */
    public static int getVerseCountByCategory(String category) {
        int count = 0;

        for (VerseData verse : verses) {
            if (verse.getCategory().equals(category)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Search verses by text (Arabic or English)
     */
    public static List<VerseData> searchVerses(String query) {
        List<VerseData> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (VerseData verse : verses) {
            if (verse.getArabicText().toLowerCase().contains(lowerQuery) ||
                    verse.getEnglishTranslation().toLowerCase().contains(lowerQuery) ||
                    verse.getReference().toLowerCase().contains(lowerQuery)) {
                results.add(verse);
            }
        }

        return results;
    }

    /**
     * Get verse by index
     */
    public static VerseData getVerseByIndex(int index) {
        if (index >= 0 && index < verses.size()) {
            return verses.get(index);
        }
        return null;
    }

    /**
     * Get total verse count
     */
    public static int getTotalVerseCount() {
        return verses.size();
    }

    /**
     * Check if repository is initialized
     */
    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Get random verse from specific category
     */
    public static VerseData getRandomVerseFromCategory(String category) {
        List<VerseData> categoryVerses = getVersesByCategory(category);

        if (categoryVerses.isEmpty()) {
            return getRandomVerse(); // Fallback to any random verse
        }

        int randomIndex = random.nextInt(categoryVerses.size());
        return categoryVerses.get(randomIndex);
    }

    /**
     * Get verses for notification rotation
     * This ensures we don't repeat verses too quickly
     */
    public static VerseData getVerseForNotification(Context context) {
        if (verses.isEmpty()) {
            initialize();
        }

        // Simple approach: return random verse
        // You could implement more sophisticated logic here like:
        // - Track recently shown verses
        // - Rotate through categories
        // - Time-based selection
        return getRandomVerse();
    }
}