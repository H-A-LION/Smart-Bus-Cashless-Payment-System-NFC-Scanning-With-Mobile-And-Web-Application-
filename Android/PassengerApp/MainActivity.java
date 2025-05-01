package com.example.smartbuspayment;

import android.content.Intent;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

//import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText nameEditText,passwordEditText;
    private Button btnLogin, btnSignUp,switchphone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(MainActivity.this, MainActivity2.class));
            finish();
        }

        // Initialize views
        nameEditText = findViewById(R.id.name_login_editText);
        passwordEditText = findViewById(R.id.password_login_editText);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.signup_edittext);
        switchphone=findViewById(R.id.switch_to_phone);


        // Set click listeners
        btnLogin.setOnClickListener(v -> login());

        btnSignUp.setOnClickListener(v -> signup());

        switchphone.setOnClickListener(v -> switchToPhonePage());
    }

    public void login(){
        String email=nameEditText.getText().toString().trim();
        String password=passwordEditText.getText().toString().trim();
        authByEmail(email,password);

    }

    public void authByEmail(String email,String password){
        // Input validation
        if (email.isEmpty()) {
            nameEditText.setError("Email cannot be empty");
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password cannot be empty");
            return;
        }
        //Add this to prevent execution if email is invalid
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            nameEditText.setError("Invalid email");
            return;
        }
        // Authenticate with Firebase
        auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, MainActivity2.class));
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void switchToPhonePage(){
        startActivity(new Intent(getApplicationContext(),SigninPhone.class));
    }

    public void signup(){
        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
    }
}





