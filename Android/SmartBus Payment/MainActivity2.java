
package com.example.smartbuspayment;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.Manifest;
import androidx.activity.EdgeToEdge;
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

import com.google.firebase.auth.FirebaseAuth;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;
public class MainActivity2 extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int IMAGE_CAPTURE_ACTION_REQUEST=101;

    ImageView imageViewCaptured;

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
        //imageViewCaptured=findViewById(R.id.imageView_captured);
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
                        makePayment();
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
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        // Redirect to login activity or update UI
        startActivity(new Intent(MainActivity2.this,MainActivity.class));
    }
