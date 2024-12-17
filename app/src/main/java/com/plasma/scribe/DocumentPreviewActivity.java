package com.plasma.scribe;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DocumentPreviewActivity extends AppCompatActivity {

    private TextView title;
    private TextView docPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_document_preview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_document_preview), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        title = findViewById(R.id.document_title);
        docPreview = findViewById(R.id.doc_preview);
        title.setText(getIntent().getStringExtra("title"));
        docPreview.setText(getIntent().getStringExtra("document"));

        docPreview.setMovementMethod(new ScrollingMovementMethod());
    }
}