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

import java.util.Objects;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_forgot_password), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        Button forgotPasswordButton = findViewById(R.id.forgot_password_button_apply);
        EditText emailInput = findViewById(R.id.forgot_password_input_email);

        forgotPasswordButton.setOnClickListener(v -> {
            findViewById(R.id.progress_bar).setVisibility(ProgressBar.VISIBLE);

            String email = emailInput.getText().toString();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter your Email Address.", Toast.LENGTH_SHORT).show();
            } else {
                auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Password-Reset Link has been sent to your registered Email.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(ForgotPasswordActivity.this, SignInActivity.class));
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            findViewById(R.id.progress_bar).setVisibility(ProgressBar.INVISIBLE);
        });

        findViewById(R.id.forgot_password_text_sign_in).setOnClickListener(v ->
                startActivity(new Intent(ForgotPasswordActivity.this, SignInActivity.class))
        );
    }
}