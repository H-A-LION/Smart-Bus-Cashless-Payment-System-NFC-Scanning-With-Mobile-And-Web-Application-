package com.example.smartbuspayment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import android.widget.Toast;
public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission is already granted, proceed with camera functionality
            openCamera();
        }



    }
    private void scanQRCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan a QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(true);

        barcodeLauncher.launch(options);

    }
    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String scannedData = result.getContents();
                    if (scannedData.equals("payforbus")) {
                        Toast.makeText(this, "Correct QR Code Found!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );
    private void makePayment(){


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with camera functionality
                openCamera();
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openCamera() {
        // Start the camera functionality here


    }
}
