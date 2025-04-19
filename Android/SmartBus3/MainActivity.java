package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText nameEditText, passwordEditText;
    private Button btnLogin, btnSignUp, switchPhone;
    private TextView forgotPasswordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.name_login_editText);
        passwordEditText = findViewById(R.id.password_login_editText);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.signup_edittext);
        switchPhone = findViewById(R.id.switch_to_phone);
        forgotPasswordTextView = findViewById(R.id.btnForgotPassword);

        auth = FirebaseAuth.getInstance();

        // Auto-login if email is already verified
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && currentUser.isEmailVerified()) {
                    goToHome();
                }
            });
        }

        // Message after successful registration
        if (getIntent().getBooleanExtra("registered", false)) {
            Toast.makeText(this, "Registration successful. Please verify your email and then log in.", Toast.LENGTH_LONG).show();
        }

        btnLogin.setOnClickListener(v -> login());
        btnSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
        switchPhone.setOnClickListener(v -> startActivity(new Intent(this,SignPhoneActivity.class)));
        forgotPasswordTextView.setOnClickListener(v -> startActivity(new Intent(this, ForgetPasswordActivity.class)));
    }

    private void login() {
        String email = nameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            nameEditText.setError("Email is required");
            nameEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            nameEditText.setError("Invalid email format");
            nameEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.reload().addOnCompleteListener(reloadTask -> {
                                if (user.isEmailVerified()) {
                                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                                    goToHome();
                                } else {
                                    user.sendEmailVerification()
                                            .addOnSuccessListener(unused -> Toast.makeText(this,
                                                    "Please verify your email. A verification link has been sent.",
                                                    Toast.LENGTH_LONG).show())
                                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Failed to send verification link: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show());
                                    auth.signOut();
                                }
                            });
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(this, "Email is not registered. Please create an account.", Toast.LENGTH_LONG).show();
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Incorrect password. Try again.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void goToHome() {
        startActivity(new Intent(this, MainActivity2.class));
        finish();
    }
}
