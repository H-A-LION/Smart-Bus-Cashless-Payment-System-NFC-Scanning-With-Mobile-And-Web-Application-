package com.example.myapplication;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity2 extends AppCompatActivity {

    private Spinner stationSpinner;
    private FirebaseFirestore db;
    private String currentUserId;
    private Button showTransactions;
    private FloatingActionButton scanpay;
    private TextView balanceTextView;
    private double temppay = 0;
    final double PRICEFORPAYMENT = 2;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "PaymentPrefs";
    private static final String TEMP_PAY_KEY = "temp_pay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bus Payment");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        temppay = sharedPreferences.getFloat(TEMP_PAY_KEY, 0.0f);

        balanceTextView = findViewById(R.id.balance);
        stationSpinner = findViewById(R.id.station_spinner);
        showTransactions = findViewById(R.id.show_transaction);
        scanpay = findViewById(R.id.floatingActionButton);

        setupStationSpinner();
        loadUserBalance();

        showTransactions.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TransactionActivity.class));
        });

        scanpay.setOnClickListener(v -> {
            scanQRCode();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        recoverPendingPayments();
    }

    private void recoverPendingPayments() {
        while (temppay > 0) {
            makePayment();
        }
    }

    private void saveTempPayment() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(TEMP_PAY_KEY, (float) temppay);
        editor.apply();
    }

    private void scanQRCode() {
        //launch properties of camera
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }
// Result the content from camera and check the content
    final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String scannedData = result.getContents().toString();
                    if (scannedData.equals("payforbus")) {
                        Toast.makeText(this, "Correct QR Code Found!", Toast.LENGTH_LONG).show();
                        makePayment();
                    } else {
                        Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private void loadUserBalance() {
        DocumentReference userRef = db.collection("agents").document(currentUserId);
        userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading balance", Toast.LENGTH_SHORT).show();
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                Double balance = documentSnapshot.getDouble("balance");
                if (balance != null) {
                    balanceTextView.setText(String.format("%.2f", balance));
                }
            }
        });
    }

    private void makePayment() {

        String paymentId = db.collection("pendingPayments").document().getId();
        java.util.Date now = new java.util.Date();

        DocumentReference userRef = db.collection("agents").document(currentUserId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Double currentBalance = documentSnapshot.getDouble("balance");

                if (currentBalance != null && currentBalance >= PRICEFORPAYMENT) {
                    // خصم الرصيد
                    double updatedBalance = currentBalance - PRICEFORPAYMENT;
                    userRef.update("balance", updatedBalance)
                            .addOnSuccessListener(unused -> {
                                // بيانات الدفع
                                Map<String, Object> paymentData = new HashMap<>();
                                paymentData.put("userId", currentUserId);
                                paymentData.put("timestamp", now);
                                paymentData.put("amount", PRICEFORPAYMENT);


                                // حفظ الدفع في pendingPayments
                                db.collection("pendingPayments").document(paymentId).set(paymentData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Payment initiated and balance deducted!", Toast.LENGTH_SHORT).show();

                                            // حفظ المعاملة في transactions
                                            Map<String, Object> transactionData = new HashMap<>(paymentData);
                                            transactionData.put("paymentId", paymentId);
                                            transactionData.put("status", "Completed");

                                            db.collection("transactions").document(paymentId).set(transactionData);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to initiate payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to retrieve balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void setupStationSpinner() {
        db.collection("stations").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> stationNames = new ArrayList<>();
                        stationNames.add("Select a station");

                        for (DocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            if (name != null) {
                                stationNames.add(name);
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stationNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        stationSpinner.setAdapter(adapter);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_profile) {
            startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            signOut();
            return true;
        } else if (item.getItemId() == R.id.menu_charge) {
            startActivity(new Intent(getApplicationContext(), ChargeBalance.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MainActivity2.this, MainActivity.class));
        finish();
    }
}
