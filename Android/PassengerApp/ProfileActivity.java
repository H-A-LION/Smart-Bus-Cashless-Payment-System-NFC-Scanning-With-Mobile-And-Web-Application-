package com.example.smartbuspayment;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;


public class ProfileActivity extends AppCompatActivity {
    private EditText tvName, tvEmail, tvPhone;
    private TextView tvBalance;
    private FirebaseFirestore db;
    private String userId;
    private Button btn_edit,btn_save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize views
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvBalance = findViewById(R.id.tv_balance);
        btn_edit=findViewById(R.id.btn_settings);
        btn_save=findViewById(R.id.btn_save);


        // Initialize Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userId = user.getUid();

        // Load user data
        loadUserData();

        btn_edit.setOnClickListener(v->switch_Views(true));
        btn_save.setOnClickListener(v ->{uploadUserData();switch_Views(false);});

    }

    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            tvName.setText(document.getString("username"));
                            tvEmail.setText(document.getString("email"));
                            tvPhone.setText(document.getString("phone"));
                            tvBalance.setText(String.format("$%.2f", document.getDouble("balance")));
                        }
                    }
                });
    }

    private void uploadUserData(){
    // Create a Map with the data to update
    Map<String, Object> userData = new HashMap<>();
    userData.put("username", tvName.getText().toString());
    userData.put("email", tvEmail.getText().toString());
    userData.put("phone", tvPhone.getText().toString());

    // Update Firestore document
    db.collection("users").document(userId)
            .set(userData, SetOptions.merge()) // merge() preserves fields not in this update
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Firestore", "Error writing document", e);
            });
        switch_Views(false);
    }

    private void switch_Views(boolean edit){
        if (edit){
            setEditTextEnabled(tvEmail,true);
            setEditTextEnabled(tvEmail,true);
            setEditTextEnabled(tvPhone,true);
            toggleViewVisibility(btn_edit,btn_save);
            }else {
            setEditTextEnabled(tvEmail,false);
            setEditTextEnabled(tvEmail,false);
            setEditTextEnabled(tvPhone,false);
            toggleViewVisibility(btn_save,btn_edit);
            }
    }

    public static void setEditTextEnabled(EditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setFocusableInTouchMode(enabled);
        editText.setClickable(enabled);
        if (enabled) {
            editText.requestFocus();
        }
    }

    public static void toggleViewVisibility(View viewToHide, View viewToShow) {
        viewToHide.setVisibility(View.GONE);
        viewToShow.setVisibility(View.VISIBLE);
    }
}

