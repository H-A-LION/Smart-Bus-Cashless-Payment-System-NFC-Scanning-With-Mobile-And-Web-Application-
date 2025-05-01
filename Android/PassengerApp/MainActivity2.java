package com.example.smartbuspayment;

import static com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BitmapCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import android.provider.MediaStore;
import android.service.controls.actions.FloatAction;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity2 extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_CAPTURE_ACTION_REQUEST=101;
    ImageView imageViewCaptured;

    private Spinner stationSpinner;
    private FirebaseFirestore db;
    private String currentUserId;
    private Button showTransactions,seeMap;
    private FloatingActionButton scanpay;

    private TextView balanceTextView;
    final double PRICEFORPAYMENT=1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        // set toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bus Payment");
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //get id of user doc
        currentUserId = user.getUid();

        // Initialize views
        balanceTextView = findViewById(R.id.balance);
        stationSpinner = findViewById(R.id.station_spinner);
        showTransactions=findViewById(R.id.show_transaction);
        scanpay=findViewById(R.id.floatingActionButton);
        seeMap=findViewById(R.id.see_map);

        // Set up UI components
        setupStationSpinner();
        loadUserBalance();
        
        // Set click listeners
        showTransactions.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TransactionsActivity.class));
        });

        seeMap.setOnClickListener(v -> openMap());

        scanpay.setOnClickListener(v -> {
            scanQRCode();
        });
    }
    private void openMap(){
        try{
            startActivity(new Intent(getApplicationContext(),MapActivity.class));
        }catch (Exception e){
            Toast.makeText(this, "Error::::::"+e.getMessage(), Toast.LENGTH_SHORT).show();

        }

    }
    private void scanQRCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR Code");
        options.setBeepEnabled(true);
        /*options.setBarcodeImageEnabled(true);*/
        options.setOrientationLocked(false);

        //launch options to barcodeLauncher
        barcodeLauncher.launch(options);

    }
//        init barcodeLauncher
        final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
                new ScanContract(),
                result -> {
                    if (result.getContents() != null ) {
                        verifyAndProcessQRPayment(result.getContents());
                    } else {
                        Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_LONG).show();
                    }
                }
        );
    private void verifyAndProcessQRPayment(String qrId) {
        DocumentReference paymentRef = db.collection("qrPayments").document(qrId);

        paymentRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String status = document.getString("status");
                Date expiresAt = document.getDate("expiresAt");
                if ("pending".equals(status) && expiresAt!=null &&expiresAt.after(new Date())){
                    processQRPayment(qrId, document.getString("busId"));
                }else {
                    Toast.makeText(this, "QR expired or invalid", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    private void processQRPayment(String qrId, String busId) {
        try {
            DocumentReference userRef = db.collection("users").document(currentUserId);
            DocumentReference paymentRef = db.collection("qrPayments").document(qrId);

            db.runTransaction(transaction -> {// declare run transaction function
                // 1. Verify QR is still valid, Error handling
                DocumentSnapshot paymentDoc = transaction.get(paymentRef);
                if (!"pending".equals(paymentDoc.getString("status"))) {
                    throw new FirebaseFirestoreException("QR already used or Expired", FAILED_PRECONDITION);
                }
                // 2. Verify user balance
                DocumentSnapshot userDoc = transaction.get(userRef);
                Double balance = userDoc.getDouble("balance");
                if (balance == null || balance < PRICEFORPAYMENT) {
                    Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
                    return null;

                }

                // 3. Update payment status
                Map<String, Object> paymentUpdates = new HashMap<>();
//                paymentUpdates.put("payment_is_done", true);
                paymentUpdates.put("status", "completed");
                paymentUpdates.put("completedAt", FieldValue.serverTimestamp());
                paymentUpdates.put("userId", currentUserId);
                transaction.update(paymentRef, paymentUpdates);

                // 4. Deduct from balance from users collection
                transaction.update(userRef, "balance", (balance - PRICEFORPAYMENT));

                // 5. Record transaction (in user's transactions subcollection)
                Map<String, Object> transactionData = new HashMap<>();
                transactionData.put("amount", PRICEFORPAYMENT);
                transactionData.put("busId", busId);
                transactionData.put("method", "QR");
                transactionData.put("qrId", qrId);
                transactionData.put("timestamp", FieldValue.serverTimestamp());

                DocumentReference txRef = userRef.collection("transactions").document();
                transaction.set(txRef, transactionData);//set data for transaction subcollection

                return null;
            }).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
                loadUserBalance(); // Refresh UI

            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            Log.e("TAG", "processQRPayment: ", e);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with camera functionality
                performOpenCamera();
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(resultCode,resultCode,data);
        if(requestCode==IMAGE_CAPTURE_ACTION_REQUEST && resultCode==RESULT_CANCELED){
            Toast.makeText(this,"Canceled",Toast.LENGTH_LONG).show();
        }
        if(requestCode==IMAGE_CAPTURE_ACTION_REQUEST && resultCode==RESULT_OK){
           //get image from intent and display it
            Bundle bundle=data.getExtras();
            Bitmap bitmap= (Bitmap) bundle.get("data");
            imageViewCaptured.setImageBitmap(bitmap);


        }

    }
    private void performOpenCamera(){
        //get the scanned image
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        startActivityForResult(intent,IMAGE_CAPTURE_ACTION_REQUEST);
    }
    private void openCamera() {
        // Start the camera functionality here
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            //permission is not granted
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION_REQUEST_CODE);
        }
        else {
            //permission already granted
            scanQRCode();
        }


    }

    private void loadUserBalance() {
        DocumentReference userRef = db.collection("users").document(currentUserId);

        userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading balance", Toast.LENGTH_SHORT).show();
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
               Double  balance = documentSnapshot.getDouble("balance");
                if (balance != null ) {
                    balanceTextView.setText(String.format("%.2f", balance));
                }
            }
        });

    }
//    private void makePayment(boolean resubmit) {
//        Log.d("Payment", "Starting payment process. Resubmit: " + resubmit);
//        try{
//            // Get references
//            DocumentReference userRef = db.collection("users").document(currentUserId);
//            Log.d("Payment", "User reference: " + userRef.getPath());
//            //take snap shot of balance in database
//            userRef.get()
//                    .addOnSuccessListener(documentSnapshot -> {
//                        Log.d("Payment", "Got user document");
//
//                        Double currentBalance=documentSnapshot.getDouble("balance");
//                        if (currentBalance==null){
//                            Toast.makeText(this, "currentbalance=null", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//
//                        if (currentBalance < PRICEFORPAYMENT) {
//                            Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//                        // Only increment temppay for new payments (not recoveries)
//
//
//                        // Calculate new balance
//                        double newBalance = currentBalance - PRICEFORPAYMENT;
//
//                        if (!resubmit) {
//                            temppay += PRICEFORPAYMENT;
//                            saveTempPayment();
//                        }
//
//                        // Create transaction data
//                        Map<String, Object> transaction = new HashMap<>();
//                        transaction.put("amount", PRICEFORPAYMENT);
//                        transaction.put("timestamp", FieldValue.serverTimestamp());
//                        transaction.put("type", "payment");
//                        transaction.put("busId", "Bus_001");
//
//                        // Create user data update
//                        Map<String, Object> userData = new HashMap<>();
//                        userData.put("balance", newBalance);
//
//                        // Create batch write to ensure both operations succeed or fail together
//                        WriteBatch batch = db.batch();
//
//                        batch.update(userRef, userData);
//
//                        // Add transaction record to user's transactions subcollection
//                        DocumentReference newTransactionRef = userRef.collection("transactions").document();
//                        batch.set(newTransactionRef, transaction);
//
//                        // Execute batch
//                        batch.commit()
//                                .addOnSuccessListener(aVoid -> {
//                                    Log.d("Payment", "Batch commit successful");
//                                    // Update UI
//                                    balanceTextView.setText(String.format("%.2f", newBalance));
//                                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
//
//                                    // Only decrement if this was a recovery
//                                    if (resubmit) {
//                                        temppay -= PRICEFORPAYMENT;
//                                        saveTempPayment();
//                                    }
//                                })
//                                .addOnFailureListener(e -> {
//                                    Log.e("Payment", "Batch failed", e);
//                                });
//
//
//                })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
//                        Log.e("Payment", "Error getting user data", e);
//                    });
//
//
//        }catch (NumberFormatException e) {
//            Toast.makeText(this, "Error processing balance", Toast.LENGTH_SHORT).show();
//            Log.e("Payment", "Number format error", e);
//        }catch (Exception e){
//            Log.e("Exception","Check The Type of exception",e);
//        }
//
//    }
    private void setupStationSpinner() {
        db.collection("stations").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
//                      1.  create list to add data
                        List<String> stationNames = new ArrayList<>();
                        stationNames.add("Select a station");

                        //add list station names if the name is not null
                        for (DocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            if (name != null) {
                                stationNames.add(name);
                            }
                        }
//                        2. create Array Adapter
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, stationNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                        3. set array adapter to station spinner to show list of stations
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
        } else if (item.getItemId()==R.id.menu_charge) {
            startActivity(new Intent(getApplicationContext(),ChargeBalance.class));
        }
        return super.onOptionsItemSelected(item);
    }
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        // Redirect to login activity or update UI
        startActivity(new Intent(MainActivity2.this,MainActivity.class));
        finish();
    }

    //missing control of textview that display how much bus needs to arrive
}

