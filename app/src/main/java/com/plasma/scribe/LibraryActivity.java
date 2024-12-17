package com.plasma.scribe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;

public class LibraryActivity extends AppCompatActivity {

    private static final String TAG = "ScribeLibraryActivity";

    private ImageButton navButtonHome;
    private ImageButton navButtonLibrary;
    private ImageButton navButtonSettings;

    private DatabaseHandler dbHandler;

    private HashMap<String, String> documents = new HashMap<>();

    private RecyclerView recyclerView;
    private LibraryRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_library);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_library), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        navButtonHome = findViewById(R.id.button_home);
        navButtonLibrary = findViewById(R.id.button_library);
        navButtonSettings = findViewById(R.id.button_profile);

        navButtonHome.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryActivity.this, HomeActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navButtonLibrary.setClickable(false);
        navButtonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryActivity.this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        dbHandler = new DatabaseHandler(LibraryActivity.this);

        dbHandler.getDocuments(new DatabaseHandler.OnDataRetrievedListener<>() {
            public void onSuccess(HashMap<String, String> docs) {
                for (String key : docs.keySet()) {
                    documents.put(key, docs.get(key));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DatabaseHandler", "Failed to retrieve user name", e);
            }
        });

        recyclerView = findViewById(R.id.library_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(LibraryActivity.this));
        adapter = new LibraryRecyclerViewAdapter(LibraryActivity.this, documents);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("documents", documents);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        documents = (HashMap<String, String>) savedInstanceState.getSerializable("documents");
    }
}