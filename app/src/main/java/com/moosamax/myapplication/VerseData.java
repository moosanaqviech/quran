package com.moosamax.myapplication;

public class VerseData {
    private String arabicText;
    private String englishTranslation;
    private String reference;
    private String category;

    private String origin;

    public VerseData(String arabicText, String englishTranslation, String reference, String category, String origin) {
        this.arabicText = arabicText;
        this.englishTranslation = englishTranslation;
        this.reference = reference;
        this.category = category;
        this.origin = origin;

    }

    // Getters
    public String getArabicText() { return arabicText; }
    public String getEnglishTranslation() { return englishTranslation; }
    public String getReference() { return reference; }
    public String getCategory() { return category; }

    public String getOrigin() {return origin;}
}
