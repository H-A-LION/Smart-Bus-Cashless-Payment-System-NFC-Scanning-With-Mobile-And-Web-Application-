package com.example.smartbuspayment;

import android.content.Intent;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
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
    }
    public void login(){
        auth=FirebaseAuth.getInstance();
        nameEditText =(EditText) findViewById(R.id.name_login_editText);
        passwordEditText =(EditText) findViewById(R.id.password_login_editText);
        String User=nameEditText.getText().toString().trim();
        String password=passwordEditText.getText().toString().trim();

        if (!User.isEmpty()){
            if (!password.isEmpty()){
                auth.signInWithEmailAndPassword(User,password)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                Toast.makeText(MainActivity.this,"Login Successful",Toast.LENGTH_LONG).show();
                                startActivity(new Intent(MainActivity.this,MainActivity2.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this,"Login Failed",Toast.LENGTH_LONG).show();
                            }
                        });

            }
        }else{
            if (User.isEmpty()) {
                nameEditText.setError("Email cannot be Empty!!");
            }
            if (password.isEmpty()) {
                passwordEditText.setError("Password cannot be Empty");
            }
        }
    }
    public void signup(){
        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
    }
}
