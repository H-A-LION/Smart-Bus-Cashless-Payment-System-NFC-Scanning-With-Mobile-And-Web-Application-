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

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    private DatabaseReference databaseReference;
    private TextView paymentStatus;
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

        //gps connect to firebase
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseReference = FirebaseDatabase.getInstance().getReference("locations");
        getLocationUpdates();

        //if payment verified
        paymentStatus = (TextView) findViewById(R.id.payment_verified);
        paymentStatus.setText("Payment Verified");
        paymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

    }
    private void getLocationUpdates() {

        LocationRequest locationRequest = LocationRequest.create();

        locationRequest.setInterval(10000); // Update every 10 seconds

        locationRequest.setFastestInterval(5000); // Fastest update

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

    }
    private LocationCallback locationCallback = new LocationCallback() {
        public void onLocationResult(LocationResult locationResult) {

            Location location = locationResult.getLastLocation();

            if (location != null) {

                DatabaseReference newRef = databaseReference.push();

                Map<String, Object> locationData = new HashMap<>();

                locationData.put("latitude", location.getLatitude());

                locationData.put("longitude", location.getLongitude());

                locationData.put("timestamp", ServerValue.TIMESTAMP); //Firebase server timestamp

                newRef.setValue(locationData);

            }

        };

    };
    private boolean paymentVerified(){

        return false;
    }
    /*
    * if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
     }

    * */
}
