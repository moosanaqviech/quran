package com.moosamax.myapplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VerseRepository {
    private static List<VerseData> verses;

    static {
        initializeVerses();
    }

    private static void initializeVerses() {
        verses = new ArrayList<>();

        verses.add(new VerseData(
                "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً",
                "Our Lord, give us good in this world and good in the hereafter.",
                "Al-Baqarah 2:201",
                "Dua & Supplication"
        ));

        verses.add(new VerseData(
                "وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا",
                "And whoever fears Allah - He will make for him a way out.",
                "At-Talaq 65:2-3",
                "Trust in Allah"
        ));

        verses.add(new VerseData(
                "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا",
                "For indeed, with hardship [will be] ease.",
                "Ash-Sharh 94:5-6",
                "Hope & Patience"
        ));

        verses.add(new VerseData(
                "وَاصْبِرْ وَمَا صَبْرُكَ إِلَّا بِاللَّهِ",
                "And be patient, and your patience is not but through Allah.",
                "An-Nahl 16:127",
                "Patience & Perseverance"
        ));

        verses.add(new VerseData(
                "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ",
                "And My mercy encompasses all things.",
                "Al-A'raf 7:156",
                "Mercy & Forgiveness"
        ));

        verses.add(new VerseData(
                "فَاذْكُرُونِي أَذْكُرْكُمْ",
                "Remember Me; I will remember you.",
                "Al-Baqarah 2:152",
                "Trust in Allah"
        ));
    }

    public static VerseData getRandomVerse() {
        Random random = new Random();
        return verses.get(random.nextInt(verses.size()));
    }

    public static List<VerseData> getAllVerses() {
        return new ArrayList<>(verses);
    }
}
