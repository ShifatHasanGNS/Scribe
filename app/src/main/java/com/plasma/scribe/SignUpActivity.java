package com.plasma.scribe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private final String TAG = "ScribeSignUpActivity";

    private FirebaseAuth auth;
    private FirebaseDatabase db;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_sign_up), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance("https://scribe-v01-default-rtdb.firebaseio.com/");

        EditText email = findViewById(R.id.sign_up_input_email);
        EditText password = findViewById(R.id.sign_up_input_password);

        Button signUp = findViewById(R.id.sign_up_button_sign_up);
        signUp.setOnClickListener(v -> {
            findViewById(R.id.sign_up_progress_bar_left).setVisibility(ProgressBar.VISIBLE);
            findViewById(R.id.sign_up_progress_bar_right).setVisibility(ProgressBar.VISIBLE);

            auth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(this, task0 -> {
                        if (task0.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Great! Now, please check your Email for Verification.", Toast.LENGTH_LONG).show();

                            Objects.requireNonNull(auth.getCurrentUser()).sendEmailVerification()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            dbRef = db.getReference(Objects.requireNonNull(auth.getUid()));
                                            dbRef.setValue(new DatabaseModel());
                                            dbRef.child("email").setValue(email.getText().toString());
                                            email.setText("");
                                            password.setText("");
                                            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(SignUpActivity.this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(SignUpActivity.this, Objects.requireNonNull(task0.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

            findViewById(R.id.sign_up_progress_bar_left).setVisibility(ProgressBar.INVISIBLE);
            findViewById(R.id.sign_up_progress_bar_right).setVisibility(ProgressBar.INVISIBLE);
        });

        findViewById(R.id.sign_up_text_sign_in).setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, SignInActivity.class))
        );
    }
}