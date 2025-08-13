package com.moosamax.myapplication;

public class VerseData {
    private String arabicText;
    private String englishTranslation;
    private String reference;
    private String category;

    public VerseData(String arabicText, String englishTranslation, String reference, String category) {
        this.arabicText = arabicText;
        this.englishTranslation = englishTranslation;
        this.reference = reference;
        this.category = category;
    }

    // Getters
    public String getArabicText() { return arabicText; }
    public String getEnglishTranslation() { return englishTranslation; }
    public String getReference() { return reference; }
    public String getCategory() { return category; }
}
