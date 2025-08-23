package com.moosamax.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ShareUtils {
    private static final String TAG = "ShareUtils";

    /**
     * Share verse as plain text
     */
    public static void shareVerseAsText(Context context, VerseData verse) {
        if (verse == null) {
            Log.e(TAG, "Cannot share null verse");
            return;
        }

        String shareText = formatVerseForSharing(verse);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Quran Verse - " + verse.getReference());

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Verse"));
            Log.d(TAG, "Shared verse as text: " + verse.getReference());
        } catch (Exception e) {
            Log.e(TAG, "Error sharing verse as text", e);
        }
    }

    /**
     * Share verse as image
     */
    public static void shareVerseAsImage(Context context, VerseData verse) {
        if (verse == null) {
            Log.e(TAG, "Cannot share null verse");
            return;
        }

        try {
            Bitmap bitmap = createVerseImage(verse);
            Uri imageUri = saveImageToCache(context, bitmap, verse.getReference());

            if (imageUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Shared from Quran Verses App");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                context.startActivity(Intent.createChooser(shareIntent, "Share Verse Image"));
                Log.d(TAG, "Shared verse as image: " + verse.getReference());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sharing verse as image", e);
            // Fallback to text sharing
            shareVerseAsText(context, verse);
        }
    }

    /**
     * Share multiple verses (for favorites or category)
     */
    public static void shareMultipleVerses(Context context, List<VerseData> verses, String title) {
        if (verses == null || verses.isEmpty()) {
            Log.e(TAG, "Cannot share empty verse list");
            return;
        }

        StringBuilder shareText = new StringBuilder();
        shareText.append(title).append("\n\n");

        for (int i = 0; i < verses.size(); i++) {
            VerseData verse = verses.get(i);
            shareText.append(i + 1).append(". ");
            shareText.append(formatVerseForSharing(verse));
            if (i < verses.size() - 1) {
                shareText.append("\n\n");
            }
        }

        shareText.append("\n\nShared from Quran Verses App");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share " + title));
            Log.d(TAG, "Shared " + verses.size() + " verses");
        } catch (Exception e) {
            Log.e(TAG, "Error sharing multiple verses", e);
        }
    }

    /**
     * Format a verse for sharing
     */
    private static String formatVerseForSharing(VerseData verse) {
        return verse.getArabicText() + "\n\n" +
                verse.getEnglishTranslation() + "\n\n" +
                "— " + verse.getReference() + "\n" +
                "Category: " + verse.getCategory();
    }

    /**
     * Create a beautiful image from verse data
     */
    private static Bitmap createVerseImage(VerseData verse) {
        int width = 800;
        int height = 600;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Background
        canvas.drawColor(Color.parseColor("#4CAF50"));

        // Paint for text
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);

        // Arabic text
        paint.setTextSize(32);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        drawTextCentered(canvas, verse.getArabicText(), width / 2, 150, paint, width - 80);

        // English text
        paint.setTextSize(20);
        paint.setTypeface(Typeface.DEFAULT);
        drawTextCentered(canvas, verse.getEnglishTranslation(), width / 2, 300, paint, width - 80);

        // Reference
        paint.setTextSize(16);
        paint.setAlpha(200);
        drawTextCentered(canvas, "— " + verse.getReference(), width / 2, 450, paint, width - 80);

        // Category
        paint.setTextSize(14);
        paint.setAlpha(150);
        drawTextCentered(canvas, "Category: " + verse.getCategory(), width / 2, 500, paint, width - 80);

        // App name
        paint.setTextSize(12);
        paint.setAlpha(100);
        drawTextCentered(canvas, "Quran Verses App", width / 2, 550, paint, width - 80);

        return bitmap;
    }

    /**
     * Draw text centered with word wrapping
     */
    private static void drawTextCentered(Canvas canvas, String text, int x, int y, Paint paint, int maxWidth) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        if (bounds.width() <= maxWidth) {
            // Text fits in one line
            canvas.drawText(text, x - bounds.width() / 2, y, paint);
        } else {
            // Need to wrap text
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int lineY = y;

            for (String word : words) {
                String testLine = line.length() == 0 ? word : line + " " + word;
                paint.getTextBounds(testLine, 0, testLine.length(), bounds);

                if (bounds.width() > maxWidth && line.length() > 0) {
                    // Draw current line and start new one
                    String currentLine = line.toString();
                    paint.getTextBounds(currentLine, 0, currentLine.length(), bounds);
                    canvas.drawText(currentLine, x - bounds.width() / 2, lineY, paint);

                    line = new StringBuilder(word);
                    lineY += paint.getTextSize() + 10;
                } else {
                    line = new StringBuilder(testLine);
                }
            }

            // Draw last line
            if (line.length() > 0) {
                String currentLine = line.toString();
                paint.getTextBounds(currentLine, 0, currentLine.length(), bounds);
                canvas.drawText(currentLine, x - bounds.width() / 2, lineY, paint);
            }
        }
    }

    /**
     * Save image to cache directory and return URI
     */
    private static Uri saveImageToCache(Context context, Bitmap bitmap, String filename) {
        try {
            File cacheDir = new File(context.getCacheDir(), "shared_images");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            String cleanFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
            File imageFile = new File(cacheDir, "verse_" + cleanFilename + ".png");

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // Use FileProvider to get shareable URI
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    imageFile
            );
        } catch (IOException e) {
            Log.e(TAG, "Error saving image to cache", e);
            return null;
        }
    }

    /**
     * Share app with others
     */
    public static void shareApp(Context context) {
        String shareText = "Check out this amazing Quran Verses app! " +
                "It has beautiful verses with translations, categories, and daily notifications.\n\n" +
                "Download it now and get inspired daily with Quranic wisdom!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Quran Verses App");

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share App"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing app", e);
        }
    }
}