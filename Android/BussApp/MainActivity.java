package com.example.buspaymentscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.location.Location;
import android.os.CountDownTimer;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
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
    private static final double DISTANCE_THRESHOLD_METERS = 30;
    private String lineId;
    private List<String> stationIds = new ArrayList<>();
    private List<Station> stations = new ArrayList<>();
    private String busId ; // Should be unique for each bus
    private TextView countdownTimerText;
    private CountDownTimer countDownTimer;
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_LOCATION_INTERVAL = 5000; // 5 seconds

    private static final Uri SUCCESS_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private static final Uri ERROR_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

    //lifecycles
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

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("busId")) {
            busId = intent.getStringExtra("busId");
            TextView busText = findViewById(R.id.selected_bus_text);
            busText.setText("Bus: " + busId);
        } else {

            startActivity(new Intent(this, Activity2.class));
            finish();
            return;
        }

        // Initialize UI
        paymentStatus = findViewById(R.id.payment_verified);
        qrCodeImage = findViewById(R.id.qr_code);
        countdownTimerText = findViewById(R.id.qr_timer);
        try{
            busId=intent.getStringExtra("busId");
            // Add this in onCreate after getting busId
            if (busId != null) {
                getBusLineAndStations();
            }
        }catch (Exception e){
            Toast.makeText(this, "ERROR: "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }


        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not available", Toast.LENGTH_LONG).show();
          //  finish();
        }

        // Initialize Firebase
        initializeFirebase();

        checkPermissions();
        initializeNewPaymentSession();

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


    // Initialize
    private void initializeFirebase() {
        // Initialize Realtime Database with persistence enabled
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);// work offline
        databaseReference = FirebaseDatabase.getInstance().getReference("buses/" + busId + "/location");

        // Keep the location data synced even when offline
        databaseReference.keepSynced(true);

        // Initialize Firestore (as you already have)
        firestoreDb = FirebaseFirestore.getInstance();

        //This allows the app to read and write even when the device is offline.

        firestoreDb.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build());

        // Add anonymous authentication
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("Firebase", "Anonymous auth failed", task.getException());
                    }
                });
    }

    private void initializeNewPaymentSession() {
        currentQrId = UUID.randomUUID().toString();
        generateQrCode(currentQrId);

        // Creat Qr payments
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("busId", busId);
        qrData.put("createdAt", FieldValue.serverTimestamp());
        qrData.put("status", "pending");
        qrData.put("amount", 1.00);
        qrData.put("expiresAt", new Date(System.currentTimeMillis() + (60*60*1000))); // Validity period of 1h

        // Upload data to firestore
        firestoreDb.collection("qrPayments").document(currentQrId)
                .set(qrData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "QR session created successfully");
                    setupPaymentListener(currentQrId);
                    startExpiryTimer(currentQrId);
                })
                .addOnFailureListener(e -> Log.w("Firestore", "QR creation failed", e));
    }





    // Standardizes UID format to match web app (uppercase, no separators)
    private String formatNfcUid(String rawUid) {
        return rawUid.replaceAll("[^a-fA-F0-9]", "").toUpperCase();
    }


    private void checkUserBalanceAndCompleteTransaction(String userId, String nfcUid) {
        DocumentReference userRef = firestoreDb.collection("users").document(userId);

        firestoreDb.runTransaction(transaction -> {
            DocumentSnapshot userDoc = transaction.get(userRef);
            double currentBalance = userDoc.getDouble("balance");

            if (currentBalance < 1.00) {
                Toast.makeText(this, "Insufficient Balance", Toast.LENGTH_SHORT).show();
                return null;
            }

            // Deduct fare from balance
            double newBalance = currentBalance - 1.00;
            transaction.update(userRef, "balance", newBalance);

            return null;
        }).addOnSuccessListener(aVoid -> {
            // Only proceed with transaction if balance update succeeded
            completeNfcTransaction(userId, nfcUid);
        }).addOnFailureListener(e -> {
            Log.e("NFC", "Balance update failed", e);
            showPaymentError(e.getMessage());
        });
    }

    private void completeNfcTransaction(String userId, String nfcId) {
        if (userId == null || nfcId.isEmpty()) {

            Log.e("NFC_ERROR", "User ID is null or empty");
            showPaymentError("Invalid user ID");

            Log.w("Firestore", "Anonymous QR payment completed");
            Toast.makeText(this, "Firestore: Anonymous QR payment completed", Toast.LENGTH_SHORT).show();
            // Handle anonymous case
            return;
        }

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("amount", 1.00);
        transaction.put("busId", busId);
        transaction.put("method", "NFC");
        transaction.put("nfcId", nfcId);
        transaction.put("type", "payment");
        transaction.put("timestamp", FieldValue.serverTimestamp());

        Log.d("NFC_DEBUG", "Saving transaction for user: " + userId);
        Log.d("NFC_DEBUG", "Transaction data: " + transaction.toString());

        // 1. Create transaction of NFC

        DocumentReference txnRef = firestoreDb.collection("users")
                .document(userId)
                .collection("transactions")
                .document();

        DocumentReference cardRef = firestoreDb.collection("users")
                .document(userId)
                .collection("cards")
                .document(nfcId);

        txnRef.set(transaction, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("NFC_SUCCESS", "Transaction saved successfully");

// تحديث وقت استخدام البطاقة
                    cardRef.update("lastUsed", FieldValue.serverTimestamp())
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("NFC_SUCCESS", "Card usage updated");
                                showPaymentSuccess();
                                // توليد QR جديد بعد النجاح (إضافة جديدة)
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    initializeNewPaymentSession();
                                }, 2000);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("NFC_ERROR", "Failed to update card", e);
                                showPaymentError("Payment succeeded but card update failed");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("NFC_ERROR", "Failed to save transaction", e);
                    showPaymentError("Transaction failed: " + e.getMessage());
                });
    }

    // Utility Methods NFC
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        byte[] uidBytes = tag.getId();
        String hexUid = bytesToHex(uidBytes);

        Log.d("NFC_DEBUG", "Raw bytes: " + Arrays.toString(uidBytes));
        Log.d("NFC_DEBUG", "Hex UID: " + hexUid);

        // Try both original and reversed byte order
        String reversedUid = new StringBuilder(hexUid).reverse().toString();
        Log.d("NFC_DEBUG", "Reversed UID: " + reversedUid);

        runOnUiThread(() -> {
            processNfcPayment(hexUid);
            // Also try reversed version if first attempt fails
            new Handler().postDelayed(() -> {
                if (paymentStatus.getText().toString().contains("Failed")) {
                    processNfcPayment(reversedUid);
                }
            }, 1000);
        });
    }

    private void processNfcPayment(String nfcUid) {
        // Normalize UID format (uppercase, no separators)
        String formattedUid = formatNfcUid(nfcUid);
        Log.d("NFC_DEBUG", "Formatted UID: " + formattedUid);

        // First try direct document access
        checkCardDocument(formattedUid, () -> {
            // If not found, try collection group query
            firestoreDb.collectionGroup("cards")
                    .whereEqualTo("uid", formattedUid)  // Assuming you have a 'uid' field
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot cardDoc = task.getResult().getDocuments().get(0);
                            handleFoundCard(cardDoc, formattedUid);
                        } else {
                            Log.d("NFC_DEBUG", "Card not found via collectionGroup query");
                            showPaymentError("Card not registered");
                        }
                    });
        });
    }

    private void checkCardDocument(String uid, Runnable onNotFound) {
        // Try direct document access first
        firestoreDb.collection("users").get().addOnSuccessListener(usersSnapshot -> {
            AtomicBoolean found = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(usersSnapshot.size());

            for (DocumentSnapshot userDoc : usersSnapshot.getDocuments()) {
                userDoc.getReference().collection("cards").document(uid)
                        .get()
                        .addOnCompleteListener(cardTask -> {
                            if (cardTask.isSuccessful() && cardTask.getResult().exists()) {
                                found.set(true);
                                handleFoundCard(cardTask.getResult(), uid);
                            }
                            latch.countDown();
                        });
            }

            new Thread(() -> {
                try {
                    latch.await();
                    if (!found.get()) {
                        runOnUiThread(onNotFound);
                    }
                } catch (InterruptedException e) {
                    runOnUiThread(() -> showPaymentError("Search interrupted"));
                }
            }).start();
        });
    }

    private void handleFoundCard(DocumentSnapshot cardDoc, String uid) {

        try {
            // 1. تسجيل معلومات البطاقة (إضافة جديدة)
            Log.d("NFC_DEBUG", "Found card at: " + cardDoc.getReference().getPath());

            // 2. التحقق من حالة البطاقة (تم تحسينه)
            Boolean isActive = cardDoc.getBoolean("isActive");
            if (isActive == null || !isActive) {
                showPaymentError("Card is inactive");
                return;
            }
            // 3. الحصول على مرجع المستخدم (تم تحسين التحقق)
            DocumentReference userRef = cardDoc.getReference().getParent().getParent();
            if (userRef == null) {
                showPaymentError("User reference not found");
                return;
            }

            // 4. تسجيل معلومات المستخدم (إضافة جديدة)
            Log.d("NFC_DEBUG", "Found user: " + userRef.getId());

            // ⭐ التعديل: استدعاء الدالة المعدلة
            checkUserBalanceAndCompleteTransaction(userRef.getId(), uid);

        } catch (Exception e) {
            Log.e("NFC_ERROR", "Card processing failed", e);
            showPaymentError("System error: " + e.getMessage());
        }
    }







//   Qr Code Generation

    private void generateQrCode(String paymentId) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(paymentId, BarcodeFormat.QR_CODE, 400, 400);
            runOnUiThread(() -> qrCodeImage.setImageBitmap(bitmap));
        } catch (WriterException e) {
            Log.e("QR", "Generation failed", e);
            showPaymentError("QR Generation Failed"); // go to this function
        }
    }
    // Add this to your existing code
    private void generateNewQRPayment() {
        currentQrId = UUID.randomUUID().toString();
        Log.d("QR_GENERATION", "Generating new QR with ID: " + currentQrId);

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("busId", busId);
        paymentData.put("createdAt", FieldValue.serverTimestamp());
        paymentData.put("status", "pending");
        paymentData.put("amount", 1.00);
        paymentData.put("expiresAt", new Date(System.currentTimeMillis() + (30*60*1000))); //after 30 minutes

        firestoreDb.collection("qrPayments").document(currentQrId)
                .set(paymentData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("QR_GENERATION", "QR document created successfully");
                    generateQrCode(currentQrId);
                    setupPaymentListener(currentQrId);
                    startExpiryTimer(currentQrId);
                })
                .addOnFailureListener(e -> {
                    Log.e("QR_GENERATION", "Failed to create QR document", e);
                });
    }

    private void startExpiryTimer(String qrId) {
        long expiryDuration = 60* 60 * 1000; // 60 minutes in milliseconds

        // Cancel the previous counter if it is working
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(expiryDuration, 1000) {
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / (60 * 1000);
                long seconds = (millisUntilFinished / 1000) % 60;
                String timeFormatted = String.format("%02d:%02d", minutes, seconds);
                countdownTimerText.setText("Expires in: " + timeFormatted);
            }

            public void onFinish() {
                countdownTimerText.setText("Expired");

                firestoreDb.collection("qrPayments").document(qrId)
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                String status = document.getString("status");
                                if ("pending".equals(status)) {
                                    document.getReference().update("status", "expired")
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("QR", "QR expired, generating new one...");
                                                generateNewQRPayment(); // regenerate QR
                                            });
                                }
                            }
                        });
            }
        }.start();
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



//    Error Handling

    // UI Helpers
    private void showPaymentSuccess() {
        try {
            paymentStatus.setText("Payment Verified");
            paymentStatus.setTextColor(Color.GREEN);
            Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show();

            // Play success sound
            Ringtone ringtone = RingtoneManager.getRingtone(this, SUCCESS_SOUND);
            if (ringtone != null) {
                ringtone.play();
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                paymentStatus.setText("Scan NFC or QR to Pay");
                paymentStatus.setTextColor(Color.RED);
                initializeNewPaymentSession();
            }, 2000);
        } catch (Exception e) {
            Log.e("Payment", "Error showing success", e);
        }
    }

    private void showPaymentError(String message) {
        try {
            paymentStatus.setText("Payment Failed");
            paymentStatus.setTextColor(Color.RED);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            // Play error sound
            Ringtone ringtone = RingtoneManager.getRingtone(this, ERROR_SOUND);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            Log.e("Payment", "Error showing error", e);
        }
    }






    //    permissions
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NFC
        };

        boolean allPermissionsGranted = true;
        //check if all permissions are granted
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        //if not granted, request them
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

        // Check if the app has permission to access fine location

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // If permission is not granted, request it from the user

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
            // Exit the method until permission is granted
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

    // Defines a callback that will be triggered when location updates are received

    private LocationCallback locationCallback = new LocationCallback() {

        public void onLocationResult(LocationResult locationResult) {

            // Called whenever new location data is available

            if (locationResult == null){

                // If the result is null, log a warning and exit the method

                Log.w("Location", "Null location result");
                return;
            }

            // Get the most recent location from the result

            Location location = locationResult.getLastLocation();
            // Update the bus location in Firebase Realtime Database
            if (location != null) {
                updateBusLocation(location);
            }
        };

    };

    private void updateBusLocation(Location location) {
        if (location.getAccuracy() > 30 || System.currentTimeMillis() - location.getTime() > 15000) {
            Log.w("Location", "Ignored low-accuracy or stale location");
//            return;
        }

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("lat", location.getLatitude());
        locationData.put("lng", location.getLongitude());
        locationData.put("timestamp", ServerValue.TIMESTAMP);

        databaseReference.setValue(locationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Location updated successfully");
                    // Check if bus is near any station
                    checkStationProximity(location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Failed to update location", e);
                    queueLocationUpdate(location);
                });
    }
    private void queueLocationUpdate(Location location) {
        // Implement a simple retry mechanism or store locally for later sync
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isNetworkAvailable()) {
                updateBusLocation(location);
            }
        }, 5000); // Retry after 5 seconds
    }
    // Add this Station class as an inner class
    private static class Station {
        String id;
        String name;
        double lat;
        double lng;

        public Station(String id, String name, double lat, double lng) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lng = lng;
        }
    }
    private void getBusLineAndStations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Get the line name assigned to the bus
        db.collection("buses").document(busId)
                .get()
                .addOnSuccessListener(busDoc -> {
                    if (busDoc.exists()) {
                        String lineName = busDoc.getString("line");
                        if (lineName != null) {
                            // 2. Find the line document with this name
                            findLineDocumentByName(lineName);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting bus line", e);
                });
    }

    private void findLineDocumentByName(String lineName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("lines")
                .whereEqualTo("name", lineName)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot lineDoc = querySnapshot.getDocuments().get(0);
                        lineId = lineDoc.getId(); // Get the actual document ID
                        List<String> stationIds = (List<String>) lineDoc.get("stations");
                        if (stationIds != null) {
                            this.stationIds = stationIds;
                            // Get details for each station
                            getStationDetails(stationIds);
                        }
                    } else {
                        Log.e("Firestore", "No line found with name: " + lineName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error finding line by name", e);
                });
    }
    private void getStationDetails(List<String> stationIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        stations.clear();

        for (String stationId : stationIds) {
            db.collection("stations").document(stationId)
                    .get()
                    .addOnSuccessListener(stationDoc -> {
                        if (stationDoc.exists()) {
                            String name = stationDoc.getString("name");
                            Double lat = stationDoc.getDouble("lat");
                            Double lng = stationDoc.getDouble("lng");

                            if (name != null && lat != null && lng != null) {
                                stations.add(new Station(stationDoc.getId(), name, lat, lng));
                                // Add bus to station's bus_coming array
                                addBusToStation(stationDoc.getId());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error getting station details", e);
                    });
        }
    }

    private void addBusToStation(String stationId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("stations").document(stationId)
                .update("bus_coming", FieldValue.arrayUnion(busId))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Bus added to station's bus_coming");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding bus to station", e);
                });
    }
    private void checkStationProximity(double busLat, double busLng) {
        for (Station station : stations) {
            float[] results = new float[1];
            Location.distanceBetween(busLat, busLng,
                    station.lat, station.lng,
                    results);

            float distanceInMeters = results[0];

            if (distanceInMeters <= DISTANCE_THRESHOLD_METERS) {
                // Bus is near this station, remove from bus_coming
                removeBusFromStation(station.id);
                break; // Only process one station at a time
            }
        }
    }
    private void removeBusFromStation(String stationId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("stations").document(stationId)
                .update("bus_coming", FieldValue.arrayRemove(busId))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Bus removed from station's bus_coming");
                    // Optional: You might want to remove this station from the list
                    stations.removeIf(station -> station.id.equals(stationId));
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error removing bus from station", e);
                });
    }


}



