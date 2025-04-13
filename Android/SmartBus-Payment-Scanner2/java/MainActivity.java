package com.example.smartbuspaymentscanner;

import android.os.Bundle;
import java.util.Map;
import java.util.HashMap;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import android.location.Location;
import android.widget.TextView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;// For GPS
    private FirebaseFirestore firestoreDb;    // For transactions
    private TextView paymentStatus;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private ImageView qrCodeImage;
    private NfcAdapter nfcAdapter;
    private boolean paymentVerified = false;

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

        // Initialize UI components
        paymentStatus = findViewById(R.id.payment_verified);
        qrCodeImage = findViewById(R.id.qr_code);

        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show();
            finish();
        }

        // Initialize Firebase
        databaseReference =FirebaseDatabase.getInstance().getReference("buses/BUS001/location"); // GPS only
        firestoreDb = FirebaseFirestore.getInstance();
        firestoreDb.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Offline support
                .build());

        // Check and request permissions
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Enable NFC reader mode
        if (nfcAdapter != null) {
            Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
            nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, options);
        }

        // Start location updates
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disable NFC reader mode
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }

        // Stop location updates
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle NFC intent
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            onTagDiscovered(tag);
        }
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NFC,
                Manifest.permission.CAMERA
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        } else {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "All permissions are required", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }
    }

    // NFC Reader Callback
    @Override
    public void onTagDiscovered(Tag tag) {
        // Process NFC payment
        processPayment("NFC_CARD_" + tag.getId().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            // Process QR payment
            processPayment(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //process payment is uncomplete
    private void processPayment(String paymentId) {
        // Toggle payment status for demo
        paymentVerified = !paymentVerified;

        runOnUiThread(() -> {
            if (paymentVerified) {
                paymentStatus.setText("Payment Verified");
                paymentStatus.setTextColor(Color.GREEN);

                // Record transaction in Firebase
                recordTransaction(paymentId, 1.00); // $1.00 fare
            } else {
                paymentStatus.setText("Payment Failed");
                paymentStatus.setTextColor(Color.RED);
            }
        });
    }

    //save transactions to firestore
    private void recordTransaction(String paymentId, double amount) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("paymentId", paymentId);
        transaction.put("amount", amount);
        transaction.put("timestamp", System.currentTimeMillis());
        transaction.put("busId","BUS001");

        firestoreDb.collection("transactions").add(transaction)
                        .addOnSuccessListener(documentReference ->
                                        Toast.makeText(getApplicationContext(),"Payment Recorded",Toast.LENGTH_SHORT).show()
                                )
                        .addOnFailureListener(e -> {
                            Toast.makeText(getApplicationContext(),"Failed: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                        });
    }
    //GPS Updates (Realtime DB)
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }


    private LocationCallback locationCallback = new LocationCallback() {
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;
            Location location = locationResult.getLastLocation();

            if (location != null) {
                Map<String, Object> locationData = new HashMap<>();

                locationData.put("lat", location.getLatitude());
                locationData.put("lng", location.getLongitude());
                locationData.put("timestamp", System.currentTimeMillis());

                databaseReference.setValue(locationData);// Update bus location in Firebase

            }

        };

    };


}



