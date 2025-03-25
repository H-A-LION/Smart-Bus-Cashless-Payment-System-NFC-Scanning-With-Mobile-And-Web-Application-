package com.example.smartbuspayment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference mDatabase;
    private EditText etusername,etEmail,etPhone,etPasswd,etConfirmPassword,signup_btn;

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

    }
    public void signup(){
        auth=FirebaseAuth.getInstance();
        etusername=(EditText) findViewById(R.id.Username);
        etEmail=(EditText) findViewById(R.id.etEmail);
        etPhone=(EditText) findViewById(R.id.etPhne);
        etPasswd=(EditText) findViewById(R.id.etPassword);
        etConfirmPassword=(EditText) findViewById(R.id.etConfirmPassword);

        String user=etusername.getText().toString().trim();
        String email=etEmail.getText().toString().trim();
        int phone=Integer.parseInt(etPhone.getText().toString().trim());
        String password=etPasswd.getText().toString().trim();
        String cnfrmpasswd=etConfirmPassword.getText().toString().trim();

        if(!password.equals(cnfrmpasswd) || password.isEmpty()){
            etConfirmPassword.setError("Wrong password");
            etPasswd.setError("Wrong password");
        }else{
            if (user.isEmpty() || password.isEmpty() ||email.isEmpty()) {
                if (user.isEmpty()) {
                    etusername.setError("name cannot be Empty!!");
                }
                if (password.isEmpty()) {
                    etPasswd.setError("Password cannot be Empty");
                }
                if(email.isEmpty())
                    etEmail.setError("Email cannot be Empty");
            }
            else {


                auth.createUserWithEmailAndPassword(user,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            writeNewUser("",user,email,phone,password);
                            Toast.makeText(SignUpActivity.this,"Signup Successful",Toast.LENGTH_LONG).show();
                            startActivity(new Intent(SignUpActivity.this,MainActivity2.class));
                        }
                        else {
                            Toast.makeText(SignUpActivity.this,"SignUP Failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }

    }

    public void writeNewUser(String userId, String name, String email, int phone, String password) {
        mDatabase= FirebaseDatabase.getInstance().getReference();
        User user = new User(name, email,phone,password);

        mDatabase.child("users").child(userId).setValue(user);
    }
    @IgnoreExtraProperties
    private class User {

        public String username;
        public String email;
        public String password;
        public int phone;


        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public User(String username, String email, int phone, String password) {
            this.username = username;
            this.email = email;
            this.password=password;
            this.phone=phone;
        }

    }
}
