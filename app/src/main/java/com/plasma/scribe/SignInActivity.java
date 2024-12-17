package com.plasma.scribe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class SignInActivity extends AppCompatActivity {

    private final String TAG = "ScribeSignInActivity";

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_sign_in), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        EditText email = findViewById(R.id.sign_in_input_email);
        EditText password = findViewById(R.id.sign_in_input_password);

        findViewById(R.id.button_sign_in).setOnClickListener(v -> {
            findViewById(R.id.sign_in_progress_bar_left).setVisibility(ProgressBar.VISIBLE);

            auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            if (Objects.requireNonNull(auth.getCurrentUser()).isEmailVerified()) {
                                Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
                                Toast.makeText(SignInActivity.this, "Welcome to Scribe!", Toast.LENGTH_SHORT).show();
                                startActivity(intent);
                            } else {
                                Toast.makeText(SignInActivity.this, "Please verify your Email first.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(SignInActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                            email.setText("");
                            password.setText("");
                        }
                    });

            findViewById(R.id.sign_in_progress_bar_left).setVisibility(ProgressBar.INVISIBLE);
        });

        findViewById(R.id.sign_in_text_sign_up).setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class))
        );

        findViewById(R.id.sign_in_text_forgot_password).setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, ForgotPasswordActivity.class))
        );
    }
}