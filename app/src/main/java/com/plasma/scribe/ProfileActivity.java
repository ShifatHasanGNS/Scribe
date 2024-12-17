package com.plasma.scribe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton navButtonHome;
    private ImageButton navButtonLibrary;
    private ImageButton navButtonSettings;

    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private DatabaseReference dbRef;

    private DatabaseHandler dbHandler;

    private ImageButton buttonSignOut;
    private ImageButton buttonEditUserName;
    private ImageButton buttonEditUserBio;
    private ConstraintLayout layoutFeedback;

    private TextView userName;
    private TextView userEmail;
    private TextView userBio;

    private final HashMap<String, Uri> fileUriMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        navButtonHome = findViewById(R.id.button_home);
        navButtonLibrary = findViewById(R.id.button_library);
        navButtonSettings = findViewById(R.id.button_profile);

        auth = FirebaseAuth.getInstance();

        db = FirebaseDatabase.getInstance("https://scribe-v01-default-rtdb.firebaseio.com/");
        dbRef = db.getReference(Objects.requireNonNull(auth.getUid()));

        dbHandler = new DatabaseHandler(ProfileActivity.this);

        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userBio = findViewById(R.id.user_bio);

        buttonSignOut = findViewById(R.id.button_sign_out);
        buttonEditUserName = findViewById(R.id.button_edit_user_name);
        buttonEditUserBio = findViewById(R.id.button_edit_user_bio);
        layoutFeedback = findViewById(R.id.layout_feedback);

        dbHandler.getName(new DatabaseHandler.OnDataRetrievedListener<>() {
            @Override
            public void onSuccess(String name) {
                userName.setText(name);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DatabaseHandler", "Failed to retrieve user name", e);
            }
        });

        dbHandler.getEmail(new DatabaseHandler.OnDataRetrievedListener<>() {
            @Override
            public void onSuccess(String email) {
                userEmail.setText(email);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DatabaseHandler", "Failed to retrieve user name", e);
            }
        });

        dbHandler.getBio(new DatabaseHandler.OnDataRetrievedListener<>() {
            @Override
            public void onSuccess(String bio) {
                userBio.setText(bio);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DatabaseHandler", "Failed to retrieve user name", e);
            }
        });

        navButtonHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navButtonLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LibraryActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navButtonSettings.setClickable(false);

        buttonSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ProfileActivity.this, SignInActivity.class));
        });

        buttonEditUserName.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.edit_display_name_dialog, null);

            EditText userDisplayName = dialogView.findViewById(R.id.user_display_name);
            ImageButton buttonUpdateDisplayName = dialogView.findViewById(R.id.button_update_display_name);

            userDisplayName.setText(userName.getText());

            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this, R.style.CustomDialogTheme);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            dialog.show();

            buttonUpdateDisplayName.setOnClickListener(u -> {
                String displayNameStr = userDisplayName.getText().toString().trim();
                if (!displayNameStr.isEmpty()) {
                    userName.setText(displayNameStr);
                    dbHandler.updateName(displayNameStr);
                    dialog.dismiss();
                } else {
                    Toast.makeText(ProfileActivity.this, "Please enter a display name.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        buttonEditUserBio.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.edit_user_bio_dialog, null);

            EditText userEditedBio = dialogView.findViewById(R.id.user_bio);
            ImageButton buttonUpdateBio = dialogView.findViewById(R.id.button_update_bio);

            userEditedBio.setText(userBio.getText());

            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this, R.style.CustomDialogTheme);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            dialog.show();

            buttonUpdateBio.setOnClickListener(u -> {
                String userBioStr = userEditedBio.getText().toString().trim();
                if (!userBioStr.isEmpty()) {
                    userBio.setText(userBioStr);
                    dbHandler.updateBio(userBioStr);
                    dialog.dismiss();
                } else {
                    Toast.makeText(ProfileActivity.this, "Please tell us about yourself.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        layoutFeedback.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.feedback_dialog, null);

            EditText userFeedback = dialogView.findViewById(R.id.user_feedback);
            ImageButton buttonSendFeedback = dialogView.findViewById(R.id.button_send_feedback);

            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this, R.style.CustomDialogTheme);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            dialog.show();

            buttonSendFeedback.setOnClickListener(u -> {
                String userFeedbackStr = userFeedback.getText().toString().trim();
                if (!userFeedbackStr.isEmpty()) {
                    dbHandler.addFeedback(userFeedbackStr);
                    dialog.dismiss();
                }
            });
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("userName", userName.getText().toString());
        outState.putString("userEmail", userEmail.getText().toString());
        outState.putString("userBio", userBio.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        userName.setText(savedInstanceState.getString("userName"));
        userEmail.setText(savedInstanceState.getString("userEmail"));
        userBio.setText(savedInstanceState.getString("userBio"));
    }
}