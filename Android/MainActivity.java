package com.example.design_android;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public boolean Login(){
        nameEditText =(EditText) findViewById(R.id.name_login_editText);
        passwordEditText =(EditText) findViewById(R.id.password_login_editText);
        String name=nameEditText.getText().toString();
        String password=passwordEditText.getText().toString();
        AccountHandler account=new AccountHandler(name,password);



        return false;
    }
    private class AccountHandler{
        String name;
        String password;

        public AccountHandler(String name , String password) {
            this.name = name;
            this.password=password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
