package com.example.smartbuspaymentscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
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

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.WriteBatch;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;// For GPS
    private FirebaseFirestore firestoreDb;    // For transactions
    private TextView paymentStatus;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private ImageView qrCodeImage;
    private NfcAdapter nfcAdapter;
    private String currentQrId;
    private String busId = "BUS001"; // Should be unique for each bus
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_LOCATION_INTERVAL = 5000; // 5 seconds

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

        // Initialize UI
        paymentStatus = findViewById(R.id.payment_verified);
        qrCodeImage = findViewById(R.id.qr_code);

        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not available", Toast.LENGTH_LONG).show();
            finish();
        }

        // Initialize Firebase
        initializeFirebase();

        checkPermissions();
        initializeNewPaymentSession();

    }
    // In your onCreate()
    private void initializeFirebase() {
        // Initialize Realtime Database with persistence enabled
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);//save auth
        databaseReference = FirebaseDatabase.getInstance().getReference("buses/" + busId + "/location");

        // Keep the location data synced even when offline
        databaseReference.keepSynced(true);

        // Initialize Firestore (as you already have)
        firestoreDb = FirebaseFirestore.getInstance();
        firestoreDb.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build());// to work offline
    }

    private void initializeNewPaymentSession() {
        currentQrId = UUID.randomUUID().toString();
        generateQrCode(currentQrId);

        // Create QR payment document
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("busId", busId);
        qrData.put("createdAt", FieldValue.serverTimestamp());
        qrData.put("status", "pending");
        qrData.put("amount", 1.00);

        firestoreDb.collection("qrPayments").document(currentQrId)
                .set(qrData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "QR session created"))
                .addOnFailureListener(e -> Log.w("Firestore", "QR creation failed", e));
    }
    private void queueLocationUpdate(Location location) {
        // Implement a simple retry mechanism or store locally for later sync
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isNetworkAvailable()) {
                updateBusLocation(location);
            }
        }, 5000); // Retry after 5 seconds
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    // NFC Reader Callback
    @Override
    public void onTagDiscovered(Tag tag) {
        String nfcId = bytesToHex(tag.getId());
        processNfcPayment(nfcId);

    }
    private void processNfcPayment(String nfcId) {
        // Find user with this active NFC card
        firestoreDb.collectionGroup("nfcCards")
                .whereEqualTo("id", nfcId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot cardDoc = task.getResult().getDocuments().get(0);
                        String userId = cardDoc.getReference().getParent().getParent().getId();
                        completeNfcTransaction(userId, nfcId);
                    } else {
                        showPaymentError("Invalid NFC Card");
                    }
                });
    }
    private void completeNfcTransaction(String userId, String nfcId) {
        WriteBatch batch = firestoreDb.batch();

        // 1. Create transaction
        DocumentReference txnRef = firestoreDb.collection("users")
                .document(userId)
                .collection("transactions")
                .document();

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("amount", 1.00);
        transaction.put("busId", busId);
        transaction.put("method", "NFC");
        transaction.put("nfcId", nfcId);
        transaction.put("timestamp", FieldValue.serverTimestamp());
        batch.set(txnRef, transaction);

        // 2. Update NFC card last used
        DocumentReference cardRef = firestoreDb.collection("users")
                .document(userId)
                .collection("nfcCards")
                .document(nfcId);
        batch.update(cardRef, "lastUsed", FieldValue.serverTimestamp());

        batch.commit()
                .addOnSuccessListener(aVoid -> showPaymentSuccess())
                .addOnFailureListener(e -> showPaymentError(e.getMessage()));
    }

    private void completeQrTransaction(String qrId, String userId) {
        WriteBatch batch = firestoreDb.batch();

        // 1. Update QR payment status
        DocumentReference qrRef = firestoreDb.collection("qrPayments").document(qrId);
        batch.update(qrRef, "status", "completed");

        // 2. Create transaction if user exists
        if (userId != null) {
            DocumentReference txnRef = firestoreDb.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .document();

            Map<String, Object> transaction = new HashMap<>();
            transaction.put("amount", 1.00);
            transaction.put("busId", busId);
            transaction.put("method", "QR");
            transaction.put("qrId", qrId);
            transaction.put("timestamp", FieldValue.serverTimestamp());
            batch.set(txnRef, transaction);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> showPaymentSuccess())
                .addOnFailureListener(e -> showPaymentError(e.getMessage()));
    }
    // UI Helpers
    private void showPaymentSuccess() {
        runOnUiThread(() -> {
            paymentStatus.setText("Payment Verified");
            paymentStatus.setTextColor(Color.GREEN);
            Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show();
            initializeNewPaymentSession();
        });
    }
    private void showPaymentError(String message) {
        runOnUiThread(() -> {
            paymentStatus.setText("Payment Failed");
            paymentStatus.setTextColor(Color.RED);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
    private void generateQrCode(String paymentId) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(paymentId, BarcodeFormat.QR_CODE, 400, 400);
            runOnUiThread(() -> qrCodeImage.setImageBitmap(bitmap));
        } catch (WriterException e) {
            Log.e("QR", "Generation failed", e);
            showPaymentError("QR Generation Failed");
        }
    }
    // Add this to your existing code
    private void generateNewQRPayment() {
        currentQrId = UUID.randomUUID().toString();

        // Create payment document in existing 'payment' collection
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("busId", busId);
        paymentData.put("createdAt", FieldValue.serverTimestamp());
        paymentData.put("status", "pending");
        paymentData.put("amount", 1.00);
        paymentData.put("expiresAt", new Date(System.currentTimeMillis() + (30*60*1000)));//30 minutes to expire

        firestoreDb.collection("qrPayments").document(currentQrId)
                .set(paymentData)
                .addOnSuccessListener(aVoid -> {
                    // Generate QR code with the payment ID
                    generateQrCode(currentQrId);
                    setupPaymentListener(currentQrId); // Listen for payment completion
                    startExpiryTimer(currentQrId); // New method
                });
    }
    private void startExpiryTimer(String qrId) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            firestoreDb.collection("qrPayment").document(qrId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if ("pending".equals(document.getString("status"))) {
                            document.getReference().update("status", "expired");
                        }
                    });
        }, 30* 60 * 1000); // 30 minutes
    }

    private void setupPaymentListener(String qrId) {
        firestoreDb.collection("qrPayments").document(qrId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null){
                        Log.e("Firestore", "Listen failed", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        if ("completed".equals(status)) {
                            String userId = snapshot.getString("userId");
                            verifyTransactionCompletion(userId, qrId);
//                            completeQrTransaction(qrId, userId);
                        }
                    }
                });
    }
    private void verifyTransactionCompletion(String userId, String qrId) {
        firestoreDb.collection("users").document(userId)
                .collection("transactions")
                .whereEqualTo("qrId", qrId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        showPaymentSuccess();
                        generateNewQRPayment();
                    } else {
                        Log.w("BusApp", "Transaction record missing for QR: " + qrId);
                    }
                });
    }
    // Utility Methods
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            // QR code scanned by passenger app would update Firestore
            // No need to process here as we're listening for changes
            Toast.makeText(this, "QR Code processed", Toast.LENGTH_SHORT).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
                Manifest.permission.INTERNET
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
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {

                    allGranted = false;
                    break;
                }
            }
            if (allGranted){// start sending locations
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                startLocationUpdates();
            }else {
                Toast.makeText(this, "All permissions are required", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

        }
    }

    //GPS Updates (Realtime DB)
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
            return;
        }
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL); // 10 seconds
        locationRequest.setFastestInterval(FASTEST_LOCATION_INTERVAL); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,Looper.getMainLooper())
                .addOnSuccessListener(aVoid -> Log.d("Location", "Location updates started"))
                .addOnFailureListener(e -> Log.e("Location", "Failed to start location updates", e));

    }
    private LocationCallback locationCallback = new LocationCallback() {
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null){
                Log.w("Location", "Null location result");
                return;
            }
            Location location = locationResult.getLastLocation();
            if (location != null) {
                updateBusLocation(location);
            }
        };

    };
    private void updateBusLocation(Location location) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("lat", location.getLatitude());
        locationData.put("lng", location.getLongitude());
        locationData.put("timestamp", ServerValue.TIMESTAMP); // Use server timestamp

        databaseReference.setValue(locationData)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Location updated successfully"))
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Failed to update location", e);
                    // Implement retry logic or error handling here
                    queueLocationUpdate(location);
                });
    }


}



