package com.example.buspaymentscanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Activity2 extends AppCompatActivity {

    private Spinner spinner;
    private Button button;

    private final List<String> busIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);

        spinner = findViewById(R.id.busId_spinner);
        button = findViewById(R.id.continue_button);
        button.setEnabled(false); // تعطيل الزر حتى تحميل البيانات


        loadBusesFromFirestore();

        button.setOnClickListener(v -> {
            if (spinner.getSelectedItem() != null) {
                String selectedBusId = spinner.getSelectedItem().toString();
                navigateToMainActivity(selectedBusId);
            } else {
                Toast.makeText(this, "اختر رقم الباص أولاً", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBusesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("buses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        busIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            busIds.add(document.getId());
                        }

                        if (busIds.isEmpty()) {
                            Toast.makeText(this, "لا يوجد باصات متاحة", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        setupSpinner();
                        button.setEnabled(true); // تمكين الزر بعد التحميل
                    } else {
                        Toast.makeText(this, "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                busIds
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void navigateToMainActivity(String busId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("busId", busId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // إغلاق النشاط الحالي لمنع التكرار
    }
}