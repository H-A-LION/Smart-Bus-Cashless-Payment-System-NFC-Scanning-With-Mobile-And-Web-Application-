package com.example.smartbuspayment;

import static com.example.smartbuspayment.PasswordValidator.*;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;
public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore db;
    private EditText etUsername,etEmail,etPhone, etPassword,etConfirmPassword;
    private Button buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etUsername = findViewById(R.id.Username);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhne);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        buttonRegister=findViewById(R.id.signup_buttn);

        // Set click listener for signup button
        buttonRegister.setOnClickListener(v -> signup());

    }

    public void signup(){
        final String username = etUsername.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            etPhone.setError("Phone is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        PasswordStrength strength = checkPasswordStrength(password);

        if (strength.strengthLevel == StrengthLevel.WEAK) {
            etPassword.setError(strength.errorMessage);
            return;
        }

        // Optionally show strength feedback to user
        if (strength.strengthLevel == StrengthLevel.MEDIUM) {
            Toast.makeText(this, "Your password is decent, but could be stronger", Toast.LENGTH_SHORT).show();
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }
        auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this,new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    // Save additional user data to Firestore
                    FirebaseUser user=auth.getCurrentUser();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("username", username);
                    userData.put("email", email);
                    userData.put("phone", phone);
                    userData.put("balance", 0.0); // Initial balance
                    userData.put("role","passenger");
                    sendEmailVerification("users",user,userData);
                }
                else {
                            Toast.makeText(SignUpActivity.this,"SignUP Failed: "+task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void sendEmailVerification(String collectionName, FirebaseUser user, Map<String,Object> mapData) {
        String userId=user.getUid();

        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            db.collection(collectionName).document(userId)
                                    .set(mapData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(SignUpActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(SignUpActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                                    });

                            Toast.makeText(SignUpActivity.this,"Signup Successful",Toast.LENGTH_LONG).show();
                            Toast.makeText(SignUpActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this,MainActivity.class));


                        } else {
                            user.delete();
                            Toast.makeText(getApplicationContext(), "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

}
