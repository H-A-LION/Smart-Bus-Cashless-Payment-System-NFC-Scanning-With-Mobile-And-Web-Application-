package com.example.myapplication;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgetPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetBtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        emailEditText = findViewById(R.id.editTextEmail);
        resetBtn = findViewById(R.id.buttonResetPassword);
        auth = FirebaseAuth.getInstance();

        resetBtn.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Enter a valid email");
                emailEditText.requestFocus();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> Toast.makeText(this,
                            "Reset link sent to your email", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Failed to send reset email: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
