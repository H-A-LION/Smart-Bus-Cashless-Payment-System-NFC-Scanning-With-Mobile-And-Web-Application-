package com.example.smartbuspayment;

import android.os.Bundle;

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
}