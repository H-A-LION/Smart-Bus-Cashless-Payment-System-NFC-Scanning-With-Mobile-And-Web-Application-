package com.example.smartbuspayment;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.firestore.SetOptions;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity2 extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_CAPTURE_ACTION_REQUEST=101;
    ImageView imageViewCaptured;
    private TextView balanceTextView;
    private Spinner stationSpinner;
    private FirebaseFirestore db;
    private String currentUserId;
    private Button showTransactions;
    private FloatingActionButton scanpay;
    private double balance=0.0;
    private double temppay=0;

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
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Initialize views
        balanceTextView = findViewById(R.id.balance);
        stationSpinner = findViewById(R.id.station_spinner);
        showTransactions=findViewById(R.id.show_transaction);
        scanpay=findViewById(R.id.floatingActionButton);

        // Set up UI components
        setupStationSpinner();
        loadUserBalance();

        // Set click listeners
        showTransactions.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TransactionsActivity.class));
        });

        scanpay.setOnClickListener(v -> {
            scanQRCode();
        });



    }

    private void scanQRCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR Code");
        options.setBeepEnabled(true);
        /*options.setBarcodeImageEnabled(true);*/
        options.setOrientationLocked(false);
        launch(options);

    }
    private void launch(Object o){
        final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
                new ScanContract(),
                result -> {
                    if (result.getContents() != null) {
                        String scannedData = result.getContents();
                        if (scannedData.equals("payforbus")) {
                            makePayment();
                            Toast.makeText(this, "Correct QR Code Found!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
        barcodeLauncher.launch((ScanOptions) o);
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
        DocumentReference userRef = db.collection("agents").document(currentUserId);
        userRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading balance", Toast.LENGTH_SHORT).show();
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                balance = documentSnapshot.getDouble("balance");
                if (balance != 0.0) {
                    balanceTextView.setText(String.format("%.2f", balance));
                }
            }
        });
    }
    private void setupStationSpinner() {
        db.collection("stations").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<String> stationNames = new ArrayList<>();
                        stationNames.add("Select a station");

                        for (DocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            if (name != null) {
                                stationNames.add(name);
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, stationNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        stationSpinner.setAdapter(adapter);
                    }
                });
    }

    //uploading transaction is missing in makePayment()
    private void makePayment() {

        if (balance>1){
            temppay+=1;
            balance-=temppay;
            // Create a Map with the data to update
            Map<String, Object> userData = new HashMap<>();
            userData.put("balance", balance);
            // Update Firestore document
            db.collection("agents").document(currentUserId)
                    .set(userData, SetOptions.merge()) // merge() preserves fields not in this update
                    .addOnSuccessListener(aVoid -> {

                       temppay=0;
                        Toast.makeText(this, "Payment Done!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        balance+=temppay;
                        Toast.makeText(this, "Error Charging Balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Error writing document", e);

                    });


       }else {
            Toast.makeText(this,"You donot have enough balance",Toast.LENGTH_LONG).show();
        }

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

