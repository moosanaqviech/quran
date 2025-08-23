package com.moosamax.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryBrowseActivity extends AppCompatActivity {

    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private LinearLayout backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_browse);

        initViews();
        loadCategories();
    }

    private void initViews() {
        categoriesRecyclerView = findViewById(R.id.categories_recycler_view);
        backButton = findViewById(R.id.back_button);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up RecyclerView
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadCategories() {
        // Get all categories from repository
        List<String> categories = VerseRepository.getAllCategories();

        // Create adapter
        categoryAdapter = new CategoryAdapter(categories, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(String category) {
                openCategoryVerses(category);
            }
        });

        categoriesRecyclerView.setAdapter(categoryAdapter);
    }

    private void openCategoryVerses(String category) {
        Intent intent = new Intent(this, CategoryVersesActivity.class);
        intent.putExtra("CATEGORY_NAME", category);
        startActivity(intent);
    }
}